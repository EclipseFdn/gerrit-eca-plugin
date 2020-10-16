/*******************************************************************************
 * Copyright (C) 2020 Eclipse Foundation
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.foundation.gerrit.validation;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an internal response for a call to this API.
 * 
 * @author Martin Lowe
 *
 */
public class ValidationResponse {
	private boolean passed;
	private int errorCount;
	private Date time;
	private Map<String, CommitStatus> commits;
	private boolean trackedProject;

	public ValidationResponse() {
		this.commits = new HashMap<>();
		this.time = new Date();
	}

	/**
	 * @return the passed
	 */
	public boolean isPassed() {
		return this.passed;
	}

	/**
	 * @param passed the passed to set
	 */
	public void setPassed(boolean passed) {
		this.passed = passed;
	}

	/**
	 * @return the errorCount
	 */
	public int getErrorCount() {
		return this.errorCount;
	}

	/**
	 * @param errorCount the errorCount to set
	 */
	public void setErrorCount(int errorCount) {
		this.errorCount = errorCount;
	}

	/**
	 * @return the time
	 */
	public Date getTime() {
		return this.time;
	}

	/**
	 * @param time the time to set
	 */
	public void setTime(Date time) {
		this.time = time;
	}

	/**
	 * @return the commits
	 */
	public Map<String, CommitStatus> getCommits() {
		return this.commits;
	}

	/**
	 * @param commits the commits to set
	 */
	public void setCommits(Map<String, CommitStatus> commits) {
		this.commits = commits;
	}

	/**
	 * @return the trackedProject
	 */
	public boolean isTrackedProject() {
		return this.trackedProject;
	}

	/**
	 * @param trackedProject the trackedProject to set
	 */
	public void setTrackedProject(boolean trackedProject) {
		this.trackedProject = trackedProject;
	}
}
