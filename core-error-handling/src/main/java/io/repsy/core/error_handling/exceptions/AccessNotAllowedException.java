package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class AccessNotAllowedException extends BaseException {
  public AccessNotAllowedException(final @NonNull String msgId) {
    super(msgId);
  }
}
