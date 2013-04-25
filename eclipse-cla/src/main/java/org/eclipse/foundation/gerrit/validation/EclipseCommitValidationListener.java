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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.reviewdb.client.Account.Id;
import com.google.gerrit.reviewdb.client.AccountExternalId;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountException;
import com.google.gerrit.server.account.AccountManager;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
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
 * <li>The contributor has a valid CLA at the time of the push; and</li>
 * <li>The commit message contains a "Signed-off-by:" statement with credentials
 * matching those of the commit author</li>
 * </ul>
 * </li>
 * <li>A contributor can push a commit if:
 * <ul>
 * <li>They have a valid CLA at the time of the push;</li>
 * <li>The commit's author credentials match the user identity;</li>
 * <li>The commit message contains a "Signed-off-by:" statement with credentials
 * matching those of the commit author</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * <p>There more is information regarding CLA requirements and workflow on the
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
	@Inject
	AccountManager accountManager;
	@Inject
	IdentifiedUser.GenericFactory factory;
	
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
		messages.add(new CommitValidationMessage(String.format("Reviewing commit: %1$s", commit.getId()), false));
		messages.add(new CommitValidationMessage(String.format("Authored by: %1$s", authorIdent.getName()), false));
		
		/*
		 * The user must have a presence in Gerrit.
		 */
		IdentifiedUser author = identifyUser(authorIdent);
		if (author == null) {
			messages.add(new CommitValidationMessage("The author does not have a Gerrit account.", true));
			messages.add(new CommitValidationMessage("All authors must either be a commiter on the project, or have a current CLA on file.", false));
			messages.add(new CommitValidationMessage("Please see http://wiki.eclipse.org/CLA", false));
			throw new CommitValidationException("The author must register with Gerrit.", messages);
		}		
		
		/*
		 * A committer can author for their own project. Anybody else
		 * needs to have a current CLA on file.
		 */
		if (isCommitter(author, project)) {
			messages.add(new CommitValidationMessage("The author is a committer on the project.", false));			
		} else if (!hasCurrentAgreement(author)) {
			messages.add(new CommitValidationMessage("The author does not have a current Contributor License Agreement (CLA) on file.", true));	
			messages.add(new CommitValidationMessage("Open your user settings in Gerrit and select \"Agreements\" to create a CLA.", false));	
			messages.add(new CommitValidationMessage("Please see http://wiki.eclipse.org/CLA", false));
			throw new CommitValidationException("A Contributor License Agreement is required.", messages);			
		}
		
		/*
		 * Only committers can push on behalf of other users. Note that, I am asking
		 * if the user (i.e. the person who is doing the actual push) is a committer.
		 */
		if (!author.getAccount().equals(user.getAccount())) {
			if (!isCommitter(user, project)) {
				messages.add(new CommitValidationMessage("You are not a project committer.", true));
				messages.add(new CommitValidationMessage("Only project committers can push on behalf of others.", true));
				messages.add(new CommitValidationMessage("Please see http://wiki.eclipse.org/CLA", false));
				throw new CommitValidationException("You must be a committer to push on behalf of others.", messages);
				
			}
		} 
		
		// TODO consider adding a control to toggle debug mode.
		// Just reject it, because that's how we roll...
		// While debugging, it's a bit of a PITA to have to keep creating new commits.
		throw new CommitValidationException("I'm sorry " + receiveEvent.user.getName() + ", I can't do that.", messages);
	}

	/**
	 * <p>Answers whether or not a user has a current committer agreement on file.
	 * This determination is made based on group membership. Answers <code>true</code>
	 * if the user is a member of the designated &quot;CLA&quot; group, or
	 * <code>false</code> otherwise.
	 * 
	 * @param user
	 * @return
	 */
	private boolean hasCurrentAgreement(IdentifiedUser user) {
		// TODO Make the group identity a configurable setting.
		
		return false;
	}

	private boolean isCommitter(IdentifiedUser user, Project project) {
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
			Id id = accountManager.lookup(AccountExternalId.SCHEME_MAILTO + author.getEmailAddress());
			if (id == null) return null;
			return factory.create(id);
		} catch (AccountException e) {
			return null;
		}
	}
}
