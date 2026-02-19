package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class ItemAlreadyExistException extends BaseException {
  public ItemAlreadyExistException(final @NonNull String msgId) {
    super(msgId);
  }
}
