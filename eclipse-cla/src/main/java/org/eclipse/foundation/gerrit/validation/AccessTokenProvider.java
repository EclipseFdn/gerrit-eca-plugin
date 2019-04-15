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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AccessTokenProvider {

	private final AccountsService accountsService;
	private final String grantType;
	private final String clientId;
	private final String clientSecret;
	private final String scope;
	private Optional<AccessToken> cachedToken;
	
	private static final Logger log = LoggerFactory.getLogger(AccessTokenProvider.class); 
	
	public AccessTokenProvider(AccountsService accountsService, String grantType, String clientId, String clientSecret, String scope) {
		this.accountsService = accountsService;
		this.grantType = grantType;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.scope = scope;
		this.cachedToken = Optional.empty();
	}
	
	Optional<AccessToken> refreshToken() {
		this.cachedToken = getTokenFromServer();
		return token();
	}
	
	Optional<AccessToken> token() {
		return this.cachedToken;
	}

	private Optional<AccessToken> getTokenFromServer() {
		log.info("Getting new token from server " + AccountsService.BASE_URL);
		CompletableFuture<AccessToken> credentials = this.accountsService.postCredentials(this.grantType, this.clientId, this.clientSecret, this.scope);
		try {
			return Optional.ofNullable(credentials.get());
		} catch (InterruptedException | ExecutionException e) {
			return Optional.empty();
		}
	}
}
