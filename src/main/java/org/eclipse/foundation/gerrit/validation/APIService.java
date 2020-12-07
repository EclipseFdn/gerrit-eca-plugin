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
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;

interface APIService {

  static final HttpUrl BASE_URL = HttpUrl.get("https://api.eclipse.org/");

	@POST("/git/eca")
	CompletableFuture<Response<ValidationResponse>> validate(
			@Body ValidationRequest request);
}
