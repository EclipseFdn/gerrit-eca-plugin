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

import java.io.IOException;
import java.util.Optional;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

final class OAuthAuthenticator implements Authenticator {
	
	static final String AUTHORIZATION = "Authorization";
	
	private final AccessTokenProvider accessTokenProvider;
	
	OAuthAuthenticator(AccessTokenProvider accessTokenProvider) {
		this.accessTokenProvider = accessTokenProvider;
	}
	
	@Override
	public Request authenticate(Route route, Response response) throws IOException {
		synchronized (this) {
			Optional<AccessToken> token = this.accessTokenProvider.token();
			Request request = response.request();
			if (request.header(AUTHORIZATION) != null) {
				System.err.println("we've already attempted to authenticate");
				// we've already attempted to authenticate 
				if (responseCount(response) >= 3) {
					System.err.println("we don't retry more than 3 times");
					// we don't retry more than 3 times
					return null;
				}
				if (token.isPresent() && token.get().bearerCredentials().equals(request.header(AUTHORIZATION))) {
					// If we already failed with these credentials, try refresh the token
					// will return null if refreshToken returns empty
					System.err.println("failed with current credentials ("+token.get()+"), try refresh the token");
					return updateAuthorizationHeader(request, this.accessTokenProvider.refreshToken());
				} else {
					// we failed with different token, let's try with current one.
					System.err.println("we failed with different token, let's try with current one.");
					return updateAuthorizationHeader(request, token);
				}
			} else {
				// first authentication try, send token
				System.err.println("first authentication try");
				if (!token.isPresent()) {
					token = this.accessTokenProvider.refreshToken();
				}
				return updateAuthorizationHeader(request, token);
			}
		}
	}
	
	private static Request updateAuthorizationHeader(Request request, Optional<AccessToken> newToken) {
		if (newToken.isPresent()) {
			return request.newBuilder()
					.header(AUTHORIZATION, newToken.get().bearerCredentials())
					.build();
		} else {
			return null;
		}
	}

	private static int responseCount(Response response) {
		int result = 1;
		while ((response = response.priorResponse()) != null) {
			result++;
		}
		return result;
	}
}