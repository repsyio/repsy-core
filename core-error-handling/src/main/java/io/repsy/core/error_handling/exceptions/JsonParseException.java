package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class JsonParseException extends BaseException {
  public JsonParseException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
