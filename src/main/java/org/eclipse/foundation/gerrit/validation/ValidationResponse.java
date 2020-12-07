/**
 * ***************************************************************************** Copyright (C) 2020
 * Eclipse Foundation
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************
 */
package org.eclipse.foundation.gerrit.validation;

import java.util.Map;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

/**
 * Represents an internal response for a call to this API.
 *
 * @author Martin Lowe
 */
@AutoValue
public abstract class ValidationResponse {
  public abstract boolean passed();

  public abstract int errorCount();

  public abstract String time();

  public abstract Map<String, CommitStatus> commits();

  public abstract boolean trackedProject();

  public static JsonAdapter<ValidationResponse> jsonAdapter(Moshi moshi) {
    return new AutoValue_ValidationResponse.MoshiJsonAdapter(moshi);
  }
}
