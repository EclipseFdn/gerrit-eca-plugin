/*********************************************************************
* Copyright (c) 2019 Eclipse Foundation and others.
* 
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.foundation.gerrit.validation;

import java.util.Optional;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
abstract class UserAccount {

	abstract int uid();

	abstract String name();

	@Nullable
	abstract String mail();

	abstract ECA eca();

	@Json(name = "is_committer")
	abstract boolean isCommitter();

	// TODO: make package-private
	public static JsonAdapter<UserAccount> jsonAdapter(Moshi moshi) {
		return new AutoValue_UserAccount.MoshiJsonAdapter(moshi).nullSafe();
	}

	static Builder builder() {
		return new AutoValue_UserAccount.Builder();
	}

	@AutoValue.Builder
	abstract static class Builder {
		abstract Builder uid(int uid);

		abstract Builder name(String name);

		abstract Builder mail(String mail);

		abstract Builder eca(ECA eca);

		abstract Builder isCommitter(boolean isCommitter);

		abstract UserAccount build();
	}
}
