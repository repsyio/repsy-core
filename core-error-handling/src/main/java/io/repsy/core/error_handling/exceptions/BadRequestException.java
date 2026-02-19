package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class BadRequestException extends BaseException {
  public BadRequestException(final @NonNull String msgId) {
    super(msgId);
  }
}
