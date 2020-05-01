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

import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;
import com.squareup.moshi.JsonAdapter;

@MoshiAdapterFactory
abstract class JsonAdapterFactory implements JsonAdapter.Factory {

  static JsonAdapter.Factory create() {
    return new AutoValueMoshi_JsonAdapterFactory();
  }
}
