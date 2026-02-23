package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class SslContextInitializationException extends BaseException {
  public SslContextInitializationException(
      final @NonNull String message, final @NonNull Exception ex) {
    super(message, ex);
  }
}
