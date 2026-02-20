package io.repsy.core.error_handling.exceptions;

public class EventResponseException extends BaseException {
  public EventResponseException(String message, Exception ex) {
    super(message, ex);
  }
}
