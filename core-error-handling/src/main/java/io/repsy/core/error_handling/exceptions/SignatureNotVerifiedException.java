package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class SignatureNotVerifiedException extends BaseException {
  public SignatureNotVerifiedException(final @NonNull String msgId) {
    super(msgId);
  }
}
