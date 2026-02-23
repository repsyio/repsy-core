package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class EventResponseException extends BaseException {
  public EventResponseException(final @NonNull String message, final @NonNull Exception ex) {
    super(message, ex);
  }
}
