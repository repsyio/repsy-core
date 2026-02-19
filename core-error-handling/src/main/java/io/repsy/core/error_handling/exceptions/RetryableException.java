package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class RetryableException extends RuntimeException {
  public RetryableException(final @NonNull String message) {
    super(message);
  }
}
