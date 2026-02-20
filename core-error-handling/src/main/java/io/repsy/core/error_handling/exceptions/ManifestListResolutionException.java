package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class ManifestListResolutionException extends BaseException {
  public ManifestListResolutionException(final @NonNull String message) {
    super(message);
  }

  public ManifestListResolutionException(
      final @NonNull String message, final @NonNull Throwable cause) {
    super(message, cause);
  }
}
