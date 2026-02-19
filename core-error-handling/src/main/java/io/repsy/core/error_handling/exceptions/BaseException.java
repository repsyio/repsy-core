package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

class BaseException extends RuntimeException {
  BaseException(final @NonNull String msgId) {
    super(msgId);
  }

  BaseException(final @NonNull String message, final Throwable cause) {
    super(message, cause);
  }
}
