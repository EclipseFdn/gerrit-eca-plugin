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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import okhttp3.HttpUrl;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

interface APIService {

  static final HttpUrl BASE_URL = HttpUrl.get("https://api.eclipse.org/");

  @GET("/account/profile")
  CompletableFuture<Response<List<UserAccount>>> search(
      @Query("uid") Integer uid, @Query("name") String name, @Query("mail") String mail);

  @GET("/account/profile/{name}")
  CompletableFuture<Response<UserAccount>> search(@Path("name") String name);

  @GET("/account/profile/{name}/eca")
  CompletableFuture<Response<ECA>> eca(@Path("name") String name);

  @GET("/bots")
  CompletableFuture<Response<List<Bot>>> bots(@Query("q") String query);
}
