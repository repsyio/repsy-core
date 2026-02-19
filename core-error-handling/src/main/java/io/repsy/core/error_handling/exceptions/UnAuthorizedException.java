package io.repsy.core.error_handling.exceptions;

import java.util.Map;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Getter
public class UnAuthorizedException extends BaseException {
  private @Nullable Map<String, String> headers;

  public UnAuthorizedException(
      final @NonNull String msgId, final @Nullable Map<String, String> headers) {
    super(msgId);
    this.headers = headers;
  }

  public UnAuthorizedException(final @NonNull String msgId) {
    super(msgId);
  }
}
