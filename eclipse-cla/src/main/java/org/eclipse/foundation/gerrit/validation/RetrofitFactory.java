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

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.squareup.moshi.Moshi;

import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.internal.Util;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

final class RetrofitFactory {

	private final OkHttpClient client;
	private final MoshiConverterFactory moshiConverterFactory;

	RetrofitFactory(String grantType, String clientId, String clientSecret, String scope) {
		Moshi moshi = new Moshi.Builder()
				.add(JsonAdapterFactory.create())
				.build();
		this.moshiConverterFactory = MoshiConverterFactory.create(moshi);

		OkHttpClient baseClient = new OkHttpClient.Builder()
				.callTimeout(Duration.ofSeconds(5))
				.dispatcher(new Dispatcher(new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
	          new SynchronousQueue<>(), Util.threadFactory("OkHttp Dispatcher", true))))
				.build();
		AccountsService accountsService =  newRetrofit(AccountsService.BASE_URL, baseClient).create(AccountsService.class);
		AccessTokenProvider accessTokenProvider = new AccessTokenProvider(accountsService, grantType, clientId, clientSecret, scope);
		
		this.client = baseClient.newBuilder()
				.authenticator(new OAuthAuthenticator(accessTokenProvider)).build();
	}

	private Retrofit newRetrofit(HttpUrl baseUrl, OkHttpClient client) {
		return new Retrofit.Builder()
				.baseUrl(baseUrl)
				.callbackExecutor(Executors.newSingleThreadExecutor())
				.addConverterFactory(this.moshiConverterFactory)
				.client(client)
				.build();
	}
	
	public <T> T newService(HttpUrl baseUrl, Class<T> serviceClass) {
		return newRetrofit(baseUrl, this.client).create(serviceClass);
	}
}
