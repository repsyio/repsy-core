package io.repsy.core.error_handling.exceptions;

public class EventTimeoutException extends RuntimeException {
  public EventTimeoutException(String message) {
    super(message);
  }
}
