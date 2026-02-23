package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class EventTimeoutException extends RuntimeException {
  public EventTimeoutException(final @NonNull String message) {
    super(message);
  }
}
