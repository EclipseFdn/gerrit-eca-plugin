/**
 * ******************************************************************* Copyright (c) 2019 Eclipse
 * Foundation and others.
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 * ********************************************************************
 */
package org.eclipse.foundation.gerrit.validation;

import java.util.concurrent.CompletableFuture;
import okhttp3.HttpUrl;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

interface AccountsService {

  static final HttpUrl BASE_URL = HttpUrl.get("https://accounts.eclipse.org/");

  @FormUrlEncoded
  @POST("oauth2/token")
  CompletableFuture<AccessToken> postCredentials(
      @Field("grant_type") String grantType,
      @Field("client_id") String clientId,
      @Field("client_secret") String clientSecret,
      @Field("scope") String scope);
}
