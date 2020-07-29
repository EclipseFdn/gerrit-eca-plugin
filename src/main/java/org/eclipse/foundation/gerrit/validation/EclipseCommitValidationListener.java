/**
 * ***************************************************************************** Copyright (c) 2013
 * Eclipse Foundation and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Wayne Beaton (Eclipse Foundation)- initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.foundation.gerrit.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gerrit.server.account.externalids.ExternalIds;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import retrofit2.Response;

/**
 * The EclipseCommitValidationListener implements CommitValidationListener to ensure that project
 * committer or contributor have a valid ECA at the time of the push.
 *
 * <p>There more is information regarding ECA requirements and workflow on the <a
 * href="http://wiki.eclipse.org/CLA/Implementation_Requirements">Eclipse Wiki</a>.
 *
 * <p>The CommitValidationListener is not defined as part of the extension API, which means that we
 * need to build this as a version-sensitive <a
 * href="http://gerrit-documentation.googlecode.com/svn/Documentation/2.6/dev-plugins.html">Gerrit
 * plugin</a>.
 */
@Listen
@Singleton
public class EclipseCommitValidationListener implements CommitValidationListener {
  private static final String PLUGIN_NAME = "eclipse-eca-validation";

  private static final String CFG__GRANT_TYPE = "grantType";
  private static final String CFG__GRANT_TYPE_DEFAULT = "client_credentials";

  private static final String CFG__SCOPE = "scope";
  private static final String CFG__SCOPE_DEFAULT = "eclipsefdn_view_all_profiles";

  private static final String CFG__CLIENT_SECRET = "clientSecret";
  private static final String CFG__CLIENT_ID = "clientId";

  private static final Logger log = LoggerFactory.getLogger(EclipseCommitValidationListener.class);
  private static final String ECA_DOCUMENTATION = "Please see http://wiki.eclipse.org/ECA";

  private final ExternalIds externalIds;
  private final IdentifiedUser.GenericFactory factory;
  private final APIService apiService;

  @Inject
  public EclipseCommitValidationListener(
      ExternalIds externalIds,
      IdentifiedUser.GenericFactory factory,
      PluginConfigFactory cfgFactory) {
    this.externalIds = externalIds;
    this.factory = factory;
    PluginConfig config = cfgFactory.getFromGerritConfig(PLUGIN_NAME, true);
    RetrofitFactory retrofitFactory =
        new RetrofitFactory(
            config.getString(CFG__GRANT_TYPE, CFG__GRANT_TYPE_DEFAULT),
            config.getString(CFG__CLIENT_ID),
            config.getString(CFG__CLIENT_SECRET),
            config.getString(CFG__SCOPE, CFG__SCOPE_DEFAULT));
    this.apiService = retrofitFactory.newService(APIService.BASE_URL, APIService.class);
  }

  /**
   * Validate a single commit (this listener will be invoked for each commit in a push operation).
   */
  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {

    RevCommit commit = receiveEvent.commit;
    PersonIdent authorIdent = commit.getAuthorIdent();

    List<CommitValidationMessage> messages = new ArrayList<>();
    addSeparatorLine(messages);
    messages.add(
        new CommitValidationMessage(
            String.format("Reviewing commit: %1$s", commit.abbreviate(8).name()), false));
    messages.add(
        new CommitValidationMessage(
            String.format(
                "Authored by: %1$s <%2$s>", authorIdent.getName(), authorIdent.getEmailAddress()),
            false));
    addEmptyLine(messages);

    /*
     * Retrieve the authors Gerrit identity if it exists
     */
    Optional<IdentifiedUser> author = identifyUser(authorIdent);
    if (!author.isPresent()) {
      messages.add(
          new CommitValidationMessage("The author does not have a Gerrit account.", false));
    }

    List<String> errors = new ArrayList<>();
    if (hasCurrentAgreement(authorIdent, author)) {
      messages.add(
          new CommitValidationMessage(
              "The author has a current Eclipse Contributor Agreement (ECA) on file.", false));
    } else {
      if (isABot(authorIdent, author)) {
        messages.add(new CommitValidationMessage("The author is a registered bot and does not need an ECA.", false));
      } else {
        messages.add(
            new CommitValidationMessage(
                "The author does not have a current Eclipse Contributor Agreement (ECA) on file.\n"
                    + "If there are multiple commits, please ensure that each author has a ECA.",
                true));
        addEmptyLine(messages);
        errors.add("An Eclipse Contributor Agreement is required.");
      }
    }

    // TODO Extend exception-throwing delegation to include all possible messages.
    if (!errors.isEmpty()) {
      addDocumentationPointerMessage(messages);
      throw new CommitValidationException(errors.get(0), messages);
    }

    messages.add(new CommitValidationMessage("This commit passes Eclipse validation.", false));

    return messages;
  }

  private boolean isABot(PersonIdent authorIdent, Optional<IdentifiedUser> author) throws CommitValidationException {
    try {
      if (author.isPresent()) {
        Response<List<Bot>> bots = this.apiService.bots(author.get().getUserName().get()).get();
        if (bots.isSuccessful()) return bots.body().stream().anyMatch(b -> b.email() != null && b.email().equals(authorIdent.getEmailAddress()));
      }

      // Start a request for all emails, if any match, considered the user a bot
      Set<String> emailAddresses = new HashSet<>();
      emailAddresses.add(authorIdent.getEmailAddress());

      // add all Gerrit email addresses if present
      author.ifPresent(u -> emailAddresses.addAll(u.getEmailAddresses()));
      List<CompletableFuture<Response<List<Bot>>>> searches =
          emailAddresses.stream()
              .map(email -> this.apiService.bots(email))
              .collect(Collectors.toList());

      return anyMatch(
              searches, e -> e.isSuccessful() && e.body().stream().anyMatch(a -> a.email() != null && a.email().equals(authorIdent.getEmailAddress())))
          .get()
          .booleanValue();
    } catch (ExecutionException e) {
      log.error(e.getMessage(), e);
      throw new CommitValidationException(
          "An error happened while checking if user is a registered bot", e);
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
      Thread.currentThread().interrupt();
      throw new CommitValidationException(
          "Verification whether user is a registered bot has been interrupted", e);
    }
  }

  private static void addSeparatorLine(List<CommitValidationMessage> messages) {
    messages.add(new CommitValidationMessage("----------", false));
  }

  private static void addEmptyLine(List<CommitValidationMessage> messages) {
    messages.add(new CommitValidationMessage("", false));
  }

  private static void addDocumentationPointerMessage(List<CommitValidationMessage> messages) {
    messages.add(new CommitValidationMessage(ECA_DOCUMENTATION, false));
  }

  /**
   * Answers whether or not a user has a current committer agreement on file. This determination is
   * made based on group membership. Answers <code>true</code> if the user is a member of the
   * designated &quot;ECA&quot; group, or <code>false</code> otherwise.
   *
   * @param userIdent Object representation of user credentials of a Git commit.
   * @param user a Gerrit user if present.
   * @return <code>true</code> if the user has a current agreement, or <code>false</code> otherwise.
   * @throws CommitValidationException
   * @throws IOException
   */
  private boolean hasCurrentAgreement(PersonIdent userIdent, Optional<IdentifiedUser> user)
      throws CommitValidationException {
    if (hasCurrentAgreementOnServer(userIdent, user)) {
      if (user.isPresent()) {
        log.info(
            "User with Gerrit accound ID '"
                + user.get().getAccountId().get()
                + "' is considered having an agreement by "
                + APIService.BASE_URL);
      } else {
        log.info(
            "User with Git email address '"
                + userIdent.getEmailAddress()
                + "' is considered having an agreement by "
                + APIService.BASE_URL);
      }

      return true;
    } else {
      if (user.isPresent()) {
        log.info(
            "User with Gerrit accound ID '"
                + user.get().getAccountId().get()
                + "' is not considered having an agreement by "
                + APIService.BASE_URL);
      } else {
        log.info(
            "User with Git email address '"
                + userIdent.getEmailAddress()
                + "' is not considered having an agreement by "
                + APIService.BASE_URL);
      }
    }

    if (user.isPresent()) {
      log.info(
          "User with Gerrit accound ID '"
              + user.get().getAccountId().get()
              + "' is *not* considered having any agreement");
    } else {
      log.info(
          "User with Git email address '"
              + userIdent.getEmailAddress()
              + "' is *not* considered having any agreement");
    }
    return false;
  }

  private boolean hasCurrentAgreementOnServer(
      PersonIdent authorIdent, Optional<IdentifiedUser> user) throws CommitValidationException {
    try {
      if (user.isPresent()) {
        Response<ECA> eca = this.apiService.eca(user.get().getUserName().get()).get();
        if (eca.isSuccessful()) return eca.body().signed();
      }

      // Start a request for all emails, if any match, considered the user having an agreement
      Set<String> emailAddresses = new HashSet<>();
      emailAddresses.add(authorIdent.getEmailAddress());

      // add all Gerrit email addresses if present
      user.ifPresent(u -> emailAddresses.addAll(u.getEmailAddresses()));
      List<CompletableFuture<Response<List<UserAccount>>>> searches =
          emailAddresses.stream()
              .map(email -> this.apiService.search(null, null, email))
              .collect(Collectors.toList());

      return anyMatch(
              searches, e -> e.isSuccessful() && e.body().stream().anyMatch(a -> a.eca().signed()))
          .get()
          .booleanValue();
    } catch (ExecutionException e) {
      log.error(e.getMessage(), e);
      throw new CommitValidationException(
          "An error happened while checking if user has a signed agreement", e);
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
      Thread.currentThread().interrupt();
      throw new CommitValidationException(
          "Verification whether user has a signed agreement has been interrupted", e);
    }
  }

  static <T> CompletableFuture<Boolean> anyMatch(
      List<? extends CompletionStage<? extends T>> l, Predicate<? super T> criteria) {
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    Consumer<T> whenMatching =
        v -> {
          if (criteria.test(v)) result.complete(Boolean.TRUE);
        };
    CompletableFuture.allOf(
            l.stream().map(f -> f.thenAccept(whenMatching)).toArray(CompletableFuture<?>[]::new))
        .whenComplete(
            (ignored, t) -> {
              if (t != null) result.completeExceptionally(t);
              else result.complete(Boolean.FALSE);
            });

    return result;
  }

  /**
   * Answers the Gerrit identity (instance of IdentifiedUser) associated with the author
   * credentials, or <code>Optional.empty()</code> if the user cannot be matched to a Gerrit user
   * identity.
   *
   * @param author Object representation of user credentials of a Git commit.
   * @return an instance of IdentifiedUser or <code>null</code> if the user cannot be identified by
   *     Gerrit.
   */
  private Optional<IdentifiedUser> identifyUser(PersonIdent author) {
    try {
      /*
       * The gerrit: scheme is, according to documentation on AccountExternalId,
       * used for LDAP, HTTP, HTTP_LDAP, and LDAP_BIND usernames (that documentation
       * also acknowledges that the choice of name was suboptimal.
       *
       * We look up both using mailto: and gerrit:
       */
      Optional<ExternalId> id =
          externalIds.get(
              ExternalId.Key.create(ExternalId.SCHEME_MAILTO, author.getEmailAddress()));
      if (!id.isPresent()) {
        id =
            externalIds.get(
                ExternalId.Key.create(
                    ExternalId.SCHEME_GERRIT, author.getEmailAddress().toLowerCase()));
        if (!id.isPresent()) {
          return Optional.empty();
        }
      }
      return Optional.of(factory.create(id.get().accountId()));
    } catch (ConfigInvalidException | IOException e) {
      log.error("Cannot retrieve external id", e);
      return Optional.empty();
    }
  }
}
