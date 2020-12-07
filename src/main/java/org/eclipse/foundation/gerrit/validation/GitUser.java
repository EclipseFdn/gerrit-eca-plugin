/**
 * Copyright (C) 2020 Eclipse Foundation
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.foundation.gerrit.validation;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

/**
 * Basic object representing a Git users data required for verification written with AutoValue.
 *
 * @author Martin Lowe
 */
@AutoValue
public abstract class GitUser {
  public abstract String name();

  public abstract String mail();

  public static JsonAdapter<GitUser> jsonAdapter(Moshi moshi) {
    return new AutoValue_GitUser.MoshiJsonAdapter(moshi);
  }

  static Builder builder() {
    return new AutoValue_GitUser.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder name(String name);

    abstract Builder mail(String mail);

    abstract GitUser build();
  }
}
