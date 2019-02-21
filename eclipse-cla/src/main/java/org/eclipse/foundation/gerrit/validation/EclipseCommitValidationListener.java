/*******************************************************************************
* Copyright (c) 2013 Eclipse Foundation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Wayne Beaton (Eclipse Foundation)- initial API and implementation
*******************************************************************************/
package org.eclipse.foundation.gerrit.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.FooterKey;
import org.eclipse.jgit.revwalk.FooterLine;
import org.eclipse.jgit.revwalk.RevCommit;

import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.reviewdb.client.Account.Id;
import com.google.gerrit.reviewdb.client.AccountExternalId;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountException;
import com.google.gerrit.server.account.AccountManager;
import com.google.gerrit.server.account.GroupCache;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.RefControl;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * <p>
 * The EclipseCommitValidationListener implements CommitValidationListener to
 * ensure that commits made against Eclipse Gerrit meet contribution tracking
 * requirements.
 * </p>
 *
 * <p>
 * To summarize:
 * </p>
 *
 * <ul>
 * <li>A project committer can push a commit on behalf of themselves or any
 * other project committer;</li>
 * <li>A project committer can push a commit on behalf of a contributor if:
 * <ul>
 * <li>The contributor has a valid ECA at the time of the push; and</li>
 * <li>The commit message contains a "Signed-off-by:" statement with credentials
 * matching those of the commit author</li>
 * </ul>
 * </li>
 * <li>A contributor can push a commit if:
 * <ul>
 * <li>They have a valid ECA at the time of the push;</li>
 * <li>The commit's author credentials match the user identity;</li>
 * <li>The commit message contains a "Signed-off-by:" statement with credentials
 * matching those of the commit author</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <p>There more is information regarding ECA requirements and workflow on the
 * <a href="http://wiki.eclipse.org/CLA/Implementation_Requirements">Eclipse Wiki</a>.
 *
 * <p>
 * The CommitValidationListener is not defined as part of the extension API,
 * which means that we need to build this as a version-sensitive
 * <a href="http://gerrit-documentation.googlecode.com/svn/Documentation/2.6/dev-plugins.html">Gerrit plugin</a>.
 * </p>
 */
@Listen
@Singleton
public class EclipseCommitValidationListener implements CommitValidationListener {
	private static final String ECA_DOCUMENTATION = "Please see http://wiki.eclipse.org/ECA";
	private static final String DEFAULT_ECA_GROUP_NAME = "ldap:cn=eclipsecla,ou=group,dc=eclipse,dc=org";

	@Inject
	AccountManager accountManager;
	@Inject
	IdentifiedUser.GenericFactory factory;
	@Inject
	ProjectControl.GenericFactory projectControlFactory;
	@Inject
	GroupCache groupCache;

	/**
	 * Validate a single commit (this listener will be invoked for each commit in a
	 * push operation).
	 */
	@Override
	public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
			throws CommitValidationException {

		IdentifiedUser user = receiveEvent.user;
		Project project = receiveEvent.project;

		RevCommit commit = receiveEvent.commit;
		PersonIdent authorIdent = commit.getAuthorIdent();

		List<CommitValidationMessage> messages = new ArrayList<CommitValidationMessage>();
		addSeparatorLine(messages);
		messages.add(new CommitValidationMessage(String.format("Reviewing commit: %1$s", commit.abbreviate(8).name()), false));
		messages.add(new CommitValidationMessage(String.format("Authored by: %1$s <%2$s>", authorIdent.getName(), authorIdent.getEmailAddress()), false));
		addEmptyLine(messages);

		/*
		 * The user must have a presence in Gerrit.
		 */
		IdentifiedUser author = identifyUser(authorIdent);
		if (author == null) {
			messages.add(new CommitValidationMessage("The author does not have a Gerrit account.", true));
			messages.add(new CommitValidationMessage("All authors must either be a commiter on the project, or have a current ECA on file.", false));
			addDocumentationPointerMessage(messages);
			addEmptyLine(messages);
			throw new CommitValidationException("The author must register with Gerrit.", messages);
		}

		/*
		 * A committer can author for their own project. Anybody else
		 * needs to have a current ECA on file and sign-off on the
		 * commit.
		 */
		if (isCommitter(author, project)) {
			messages.add(new CommitValidationMessage("The author is a committer on the project.", false));
		} else {
			messages.add(new CommitValidationMessage("The author is not a committer on the project.", false));

			List<String> errors = new ArrayList<String>();
			if (hasCurrentAgreement(author)) {
				messages.add(new CommitValidationMessage("The author has a current Eclipse Contributor Agreement (ECA) on file.", false));
			} else {
				messages.add(new CommitValidationMessage("The author does not have a current Eclipse Contributor Agreement (ECA) on file.\n" +
				"If there are multiple commits, please ensure that each author has a ECA.", true));
				addEmptyLine(messages);
				errors.add("An Eclipse Contributor Agreement is required.");
			}

			if (hasSignedOff(author, commit)) {
				messages.add(new CommitValidationMessage("The author has \"signed-off\" on the contribution.", false));
			} else {
				messages.add(new CommitValidationMessage("The author has not \"signed-off\" on the contribution.\n" +
					"If there are multiple commits, please ensure that each commit is signed-off.", true));
				errors.add("The contributor must \"sign-off\" on the contribution.");
			}

			// TODO Extend exception-throwing delegation to include all possible messages.
			if (!errors.isEmpty()) {
				addDocumentationPointerMessage(messages);
				throw new CommitValidationException(errors.get(0), messages);
			}
		}

		addEmptyLine(messages);

		/*
		 * Only committers can push on behalf of other users. Note that, I am asking
		 * if the user (i.e. the person who is doing the actual push) is a committer.
		 */
		if (!author.getAccount().equals(user.getAccount())) {
			if (!isCommitter(user, project)) {
				messages.add(new CommitValidationMessage("You are not a project committer.", true));
				messages.add(new CommitValidationMessage("Only project committers can push on behalf of others.", true));
				addDocumentationPointerMessage(messages);
				addEmptyLine(messages);
				throw new CommitValidationException("You must be a committer to push on behalf of others.", messages);
			}
		}

		messages.add(new CommitValidationMessage("This commit passes Eclipse validation.", false));

		return messages;
	}

	/**
	 * Answer <code>true</code> if the identified user has signed off on the commit,
	 * or <code>false</code> otherwise. The user may use any of their identities to
	 * sign off on the commit (i.e. they can use any email address that is registered
	 * with Gerrit.
	 *
	 * @param author The Gerrit identity of the author of the commit.
	 * @param commit The commit.
	 * @return <code>true</code> if the author has signed off; <code>false</code> otherwise.
	 */
	private boolean hasSignedOff(IdentifiedUser author, RevCommit commit) {
	  for (final FooterLine footer : commit.getFooterLines()) {
          if (footer.matches(FooterKey.SIGNED_OFF_BY)) {
            final String email = footer.getEmailAddress();
            if (author.getEmailAddresses().contains(email)) return true;
          }
        }
		return false;
	}

	private void addSeparatorLine(List<CommitValidationMessage> messages) {
		messages.add(new CommitValidationMessage("----------", false));
	}

	private void addEmptyLine(List<CommitValidationMessage> messages) {
		messages.add(new CommitValidationMessage("", false));
	}

	private void addDocumentationPointerMessage(List<CommitValidationMessage> messages) {
		messages.add(new CommitValidationMessage(ECA_DOCUMENTATION, false));
	}

	/**
	 * <p>
	 * Answers whether or not a user has a current committer agreement on file.
	 * This determination is made based on group membership. Answers
	 * <code>true</code> if the user is a member of the designated
	 * &quot;ECA&quot; group, or <code>false</code> otherwise.
	 * </p>
	 *
	 * @param user
	 *            a Gerrit user.
	 * @return <code>true</code> if the user has a current agreement, or
	 *         <code>false</code> otherwise.
	 */
	private boolean hasCurrentAgreement(IdentifiedUser user) {
		return user.getEffectiveGroups().containsAnyOf(getEclipseClaGroupIds());
	}

	/**
	 * Answers a collection containing the UUIDs of groups that contain users
	 * with valid ECAs. Multiple values are supported since there is potential
	 * that--at some point in the future--we may have multiple versions of the
	 * ECA. Some period of overlap where both the old and new ECAs are valid
	 * will likely occur in that event.
	 */
	private Iterable<UUID> getEclipseClaGroupIds() {
		// TODO Make the group identities a configurable setting.
		List<UUID> groups = new ArrayList<AccountGroup.UUID>();
		groups.add(new AccountGroup.UUID(DEFAULT_ECA_GROUP_NAME));
		return groups;
	}

	/**
	 * Answers whether or not the user can push to the project. Note that
	 * this is a project in the Gerrit sense, not the Eclipse sense; we
	 * assume that any user who is authorized to push to the project
	 * repository is a committer.
	 *
	 * @param user
	 * @param project
	 * @return
	 */
	private boolean isCommitter(IdentifiedUser user, Project project) {
		try {
			/*
			 * We assume that an individual is a committer if they can push to
			 * the project.
			 */
			ProjectControl projectControl = projectControlFactory.controlFor(project.getNameKey(), user);
			RefControl refControl = projectControl.controlForRef("refs/heads/*");
			return refControl.canSubmit();
		} catch (NoSuchProjectException nspe) {
			nspe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return false;
	}

	/**
	 * Answers the Gerrit identity (instance of IdentifiedUser) associated with
	 * the author credentials, or <code>null</code> if the user cannot be
	 * matched to a Gerrit user identity.
	 *
	 * @param author
	 *            Object representation of user credentials of a Git commit.
	 * @return an instance of IdentifiedUser or <code>null</code> if the user
	 *         cannot be identified by Gerrit.
	 */
	private IdentifiedUser identifyUser(PersonIdent author) {
		try {
			/*
			 * The gerrit: scheme is, according to documentation on AccountExternalId,
			 * used for LDAP, HTTP, HTTP_LDAP, and LDAP_BIND usernames (that documentation
			 * also acknowledges that the choice of name was suboptimal.
			 *
			 * We look up both using mailto: and gerrit:
			 */
			Id id = accountManager.lookup(AccountExternalId.SCHEME_MAILTO + author.getEmailAddress());
			if (id == null)
				id = accountManager.lookup(AccountExternalId.SCHEME_GERRIT + author.getEmailAddress().toLowerCase());
			if (id == null) return null;
			return factory.create(id);
		} catch (AccountException e) {
			return null;
		}
	}
}
