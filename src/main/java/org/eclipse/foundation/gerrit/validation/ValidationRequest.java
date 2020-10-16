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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a request to validate a list of commits.
 * 
 * @author Martin Lowe
 *
 */
public class ValidationRequest {
	private URI repoUrl;
	private List<Commit> commits;
	private ProviderType provider;

	/**
	 * @return the repoUrl
	 */
	public URI getRepoUrl() {
		return this.repoUrl;
	}

	/**
	 * @param repoUrl the repoUrl to set
	 */
	public void setRepoUrl(URI repoUrl) {
		this.repoUrl = repoUrl;
	}

	/**
	 * @return the commits
	 */
	public List<Commit> getCommits() {
		return new ArrayList<>(this.commits);
	}

	/**
	 * @param commits the commits to set
	 */
	public void setCommits(List<Commit> commits) {
		this.commits = new ArrayList<>(commits);
	}

	/**
	 * @return the provider
	 */
	public ProviderType getProvider() {
		return this.provider;
	}

	/**
	 * @param provider the provider to set
	 */
	public void setProvider(ProviderType provider) {
		this.provider = provider;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ValidationRequest [repoUrl=");
		builder.append(this.repoUrl);
		builder.append(", commits=");
		builder.append(this.commits);
		builder.append(", provider=");
		builder.append(this.provider);
		builder.append("]");
		return builder.toString();
	}
	
}
