package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class ManifestSerializationException extends BaseException {
  public ManifestSerializationException(
      final @NonNull String message, final @NonNull Throwable cause) {
    super(message, cause);
  }
}
