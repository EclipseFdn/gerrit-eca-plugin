package org.eclipse.foundation.gerrit.validation;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import javax.annotation.Nullable;

@AutoValue
public abstract class Bot {

  public abstract int id();

  public abstract String projectId();

  public abstract String username();

  @Nullable
  public abstract String email();

  @Nullable
  @Json(name = "github.com")
  public abstract BotAccount gitHub();

  @Nullable
  @Json(name = "github.com-dependabot")
  public abstract BotAccount dependabot();

  @Nullable
  @Json(name = "oss.sonatype.org")
  public abstract BotAccount ossrh();

  @Nullable
  @Json(name = "docker.com")
  public abstract BotAccount dockerHub();

  public static JsonAdapter<Bot> jsonAdapter(Moshi moshi) {
    return new AutoValue_Bot.MoshiJsonAdapter(moshi);
  }
}
