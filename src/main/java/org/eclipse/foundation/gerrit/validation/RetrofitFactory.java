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

import com.squareup.moshi.Moshi;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionSpec;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.internal.Util;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

final class RetrofitFactory {
  private static final Logger log = LoggerFactory.getLogger(RetrofitFactory.class);

  static final String AUTHORIZATION = "Authorization";

  private final OkHttpClient client;
  private final MoshiConverterFactory moshiConverterFactory;

  RetrofitFactory() {
    Moshi moshi = new Moshi.Builder().add(JsonAdapterFactory.create()).build();
    this.moshiConverterFactory = MoshiConverterFactory.create(moshi);

    HttpLoggingInterceptor loggingInterceptor =
        new HttpLoggingInterceptor(
                new HttpLoggingInterceptor.Logger() {
                  @Override
                  public void log(String message) {
                    log.debug(message);
                  }
                })
            .setLevel(Level.BASIC);
    loggingInterceptor.redactHeader(AUTHORIZATION);

    this.client =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(5))
            .dispatcher(
                new Dispatcher(
                    new ThreadPoolExecutor(
                        0,
                        Integer.MAX_VALUE,
                        60,
                        TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        Util.threadFactory("OkHttp Dispatcher", true))))
            .addInterceptor(loggingInterceptor)
            // Workaround for IBM JVM compatibility (COMPATIBLE_TLS is the only profile including
            // TLS_1_0)
            .connectionSpecs(Arrays.asList(ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
            .build();
  }

  private Retrofit newRetrofit(HttpUrl baseUrl) {
    return new Retrofit.Builder()
        .baseUrl(baseUrl)
        .callbackExecutor(Executors.newSingleThreadExecutor())
        .addConverterFactory(this.moshiConverterFactory)
        .client(this.client)
        .build();
  }

  public <T> T newService(HttpUrl baseUrl, Class<T> serviceClass) {
    return newRetrofit(baseUrl).create(serviceClass);
  }
}
