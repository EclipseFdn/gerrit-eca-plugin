/*********************************************************************
* Copyright (c) 2019 Eclipse Foundation and others.
* 
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.foundation.gerrit.validation;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class AccessTokenProvider {

	private final AccountsService accountsService;
	private final String grantType;
	private final String clientId;
	private final String clientSecret;
	private final String scope;
	private Optional<AccessToken> cachedToken;
	
	public AccessTokenProvider(AccountsService accountsService, String grantType, String clientId, String clientSecret, String scope) {
		this.accountsService = accountsService;
		this.grantType = grantType;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.scope = scope;
		this.cachedToken = Optional.empty();
	}
	
	Optional<AccessToken> refreshToken() {
		cachedToken = getTokenFromServer();
		return token();
	}
	
	Optional<AccessToken> token() {
		return cachedToken;
	}

	private Optional<AccessToken> getTokenFromServer() {
		CompletableFuture<AccessToken> credentials = accountsService.postCredentials(grantType, clientId, clientSecret, scope);
		try {
			return Optional.ofNullable(credentials.get());
		} catch (InterruptedException | ExecutionException e) {
			return Optional.empty();
		}
	}
}
