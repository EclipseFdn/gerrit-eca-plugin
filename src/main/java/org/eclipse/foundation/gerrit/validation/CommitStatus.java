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

import java.util.List;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

/**
 * Contains information generated about a commit that was submitted for validation to the API.
 *
 * @author Martin Lowe
 */
@AutoValue
public abstract class CommitStatus {
  public abstract List<CommitStatusMessage> messages();

  public abstract List<CommitStatusMessage> warnings();

  public abstract List<CommitStatusMessage> errors();

  public static JsonAdapter<CommitStatus> jsonAdapter(Moshi moshi) {
    return new AutoValue_CommitStatus.MoshiJsonAdapter(moshi);
  }

  /**
   * Represents a message with an associated error or success status code.
   *
   * @author Martin Lowe
   */
  @AutoValue
  public abstract static class CommitStatusMessage {
    public abstract int code();

    public abstract String message();

    public static JsonAdapter<CommitStatusMessage> jsonAdapter(Moshi moshi) {
      return new AutoValue_CommitStatus_CommitStatusMessage.MoshiJsonAdapter(moshi);
    }
  }
}
