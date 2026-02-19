package io.repsy.core.error_handling.exceptions;

import lombok.Getter;
import org.jspecify.annotations.NonNull;

@Getter
public class RedirectToPathException extends RuntimeException {
  private final @NonNull String path;

  public RedirectToPathException(final @NonNull String path) {
    this.path = path;
  }
}
