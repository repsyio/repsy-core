package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class CryptoException extends BaseException {
  public CryptoException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
