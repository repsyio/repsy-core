package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class SubscriptionLimitReachedException extends BaseException {
  public SubscriptionLimitReachedException(final @NonNull String messageId) {
    super(messageId);
  }
}
