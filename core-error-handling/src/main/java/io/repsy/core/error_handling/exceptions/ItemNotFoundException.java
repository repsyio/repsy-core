package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class ItemNotFoundException extends BaseException {
  public ItemNotFoundException(final @NonNull String msgId) {
    super(msgId);
  }
}
