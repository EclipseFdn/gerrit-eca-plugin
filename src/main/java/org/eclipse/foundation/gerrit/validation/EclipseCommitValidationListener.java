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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.eclipse.foundation.gerrit.validation.CommitStatus.CommitStatusMessage;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.squareup.moshi.JsonAdapter;

import okhttp3.ResponseBody;
import okio.BufferedSource;
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
  private static final Logger log = LoggerFactory.getLogger(EclipseCommitValidationListener.class);
  private static final String ECA_DOCUMENTATION = "Please see http://wiki.eclipse.org/ECA";

  private final GitRepositoryManager repoManager;
  private final APIService apiService;
  private final JsonAdapter<ValidationResponse> responseAdapter;

  @Inject
  public EclipseCommitValidationListener(GitRepositoryManager repoManager) {
    this.repoManager = repoManager;
    RetrofitFactory retrofitFactory = new RetrofitFactory();
    this.apiService = retrofitFactory.newService(APIService.BASE_URL, APIService.class);
    Optional<JsonAdapter<ValidationResponse>> adapter =
        retrofitFactory.adapter(ValidationResponse.class);
    if (adapter.isEmpty()) {
      throw new IllegalStateException("Cannot process validation responses, not continuing");
    }
    this.responseAdapter = adapter.get();
  }

  /**
   * Validate a single commit (this listener will be invoked for each commit in a push operation).
   */
  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    List<CommitValidationMessage> messages = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    // create the request container
    ValidationRequest.Builder req = ValidationRequest.builder();
    req.provider("gerrit");

    // get the disk location for the project and set to the request
    try (Repository repo = this.repoManager.openRepository(receiveEvent.project.getNameKey())) {
      File indexFile = repo.getDirectory();
      String projLoc = indexFile.getAbsolutePath();
      req.repoUrl(projLoc);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new CommitValidationException(e.getMessage());
    }
    // retrieve information about the current commit
    RevCommit commit = receiveEvent.commit;
    PersonIdent authorIdent = commit.getAuthorIdent();
    PersonIdent committerIdent = commit.getCommitterIdent();

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
    // update the commit list for the request to contain the current request
    req.commits(Arrays.asList(getRequestCommit(commit, authorIdent, committerIdent)));
    // send the request and await the response from the API
    CompletableFuture<Response<ValidationResponse>> futureResponse =
        this.apiService.validate(req.build());
    try {
      Response<ValidationResponse> rawResponse = futureResponse.get();
      ValidationResponse response;
      // handle error responses (okhttp doesn't assume error types)
      if (rawResponse.isSuccessful()) {
        response = rawResponse.body();
      } else {
        // auto close the response resources after fetching
        try (ResponseBody err = futureResponse.get().errorBody();
            BufferedSource src = err.source()) {
          response = this.responseAdapter.fromJson(src);
        }
      }
      for (CommitStatus c : response.commits().values()) {
        messages.addAll(
            c.messages()
                .stream()
                .map(
                    message ->
                        new CommitValidationMessage(
                            message.message(), message.code() < 0 && response.trackedProject()))
                .collect(Collectors.toList()));
        addEmptyLine(messages);
        if (response.errorCount() > 0 && response.trackedProject()) {
          errors.addAll(
              c.errors().stream().map(CommitStatusMessage::message).collect(Collectors.toList()));
          errors.add("An Eclipse Contributor Agreement is required.");
        }
      }
    } catch (IOException | ExecutionException e) {
      log.error(e.getMessage(), e);
      throw new CommitValidationException("An error happened while checking commit", e);
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
      Thread.currentThread().interrupt();
      throw new CommitValidationException("Verification of commit has been interrupted", e);
    }

    // TODO Extend exception-throwing delegation to include all possible messages.
    if (!errors.isEmpty()) {
      addDocumentationPointerMessage(messages);
      throw new CommitValidationException(errors.get(0), messages);
    }

    messages.add(new CommitValidationMessage("This commit passes Eclipse validation.", false));
    return messages;
  }

  /**
   * Creates request representation of the commit, containing information about the current commit
   * and the users associated with it.
   *
   * @param src the commit associated with this request
   * @param author the author of the commit
   * @param committer the committer for this request
   * @return a Commit object to be posted to the ECA validation service.
   */
  private static Commit getRequestCommit(RevCommit src, PersonIdent author, PersonIdent committer) {
    // load commit object with information contained in the commit
    Commit.Builder c = Commit.builder();
    c.subject(src.getShortMessage());
    c.hash(src.name());
    c.body(src.getFullMessage());
    c.head(true);

    // get the parent commits, and retrieve their hashes
    RevCommit[] parents = src.getParents();
    List<String> parentHashes = new ArrayList<>(parents.length);
    for (RevCommit parent : parents) {
      parentHashes.add(parent.name());
    }
    c.parents(parentHashes);

    // convert the commit users to objects to be passed to ECA service
    GitUser.Builder authorGit = GitUser.builder();
    authorGit.mail(author.getEmailAddress());
    authorGit.name(author.getName());
    GitUser.Builder committerGit = GitUser.builder();
    committerGit.mail(committer.getEmailAddress());
    committerGit.name(committer.getName());

    c.author(authorGit.build());
    c.committer(committerGit.build());
    return c.build();
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
}
