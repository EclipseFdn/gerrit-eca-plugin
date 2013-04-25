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

import java.util.List;

import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
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
	@Override
	public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent) 
			throws CommitValidationException {
		// Just reject it, because that's how we roll...
		throw new CommitValidationException("I'm sorry " + receiveEvent.user.getName() + ", I can't do that.");
	}

}
