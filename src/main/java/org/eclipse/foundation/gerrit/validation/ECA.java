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
abstract class ECA {

  abstract boolean signed();

  @Json(name = "can_contribute_spec_project")
  abstract boolean canContributeToSpecProject();

  // TODO: make package-private
  public static JsonAdapter<ECA> jsonAdapter(Moshi moshi) {
    return new AutoValue_ECA.MoshiJsonAdapter(moshi);
  }

  static Builder builder() {
    return new AutoValue_ECA.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder signed(boolean signed);

    abstract Builder canContributeToSpecProject(boolean canContributeToSpecProject);

    abstract ECA build();
  }
}
