package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class ErrorOccurredException extends BaseException {
  public ErrorOccurredException(final @NonNull Exception ex) {
    super("errorOccurred", ex);
  }

  public ErrorOccurredException(final @NonNull String message, final @NonNull Exception ex) {
    super(message, ex);
  }
}
