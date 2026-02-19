package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class MfaException extends BaseException {
  public MfaException(final @NonNull String msgId) {
    super(msgId);
  }
}
