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

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
abstract class AccessToken {

  @Json(name = "access_token")
  abstract String accessToken();

  @Json(name = "expires_in")
  abstract int expiresInSeconds();

  @Json(name = "token_type")
  abstract String tokenType();

  abstract String scope();

  String bearerCredentials() {
    return "Bearer " + accessToken();
  }

  // TODO: make package-private
  public static JsonAdapter<AccessToken> jsonAdapter(Moshi moshi) {
    return new AutoValue_AccessToken.MoshiJsonAdapter(moshi);
  }

  abstract Builder toBuilder();

  static Builder builder() {
    return new AutoValue_AccessToken.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder accessToken(String accessToken);

    abstract Builder expiresInSeconds(int expireInSeconds);

    abstract Builder tokenType(String tokenType);

    abstract Builder scope(String scope);

    abstract AccessToken build();
  }
}
