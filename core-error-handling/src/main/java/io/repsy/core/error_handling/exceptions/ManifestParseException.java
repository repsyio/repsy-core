package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class ManifestParseException extends BaseException {
  public ManifestParseException(final @NonNull String message, final @NonNull Throwable cause) {
    super(message, cause);
  }
}
