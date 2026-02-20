package io.repsy.core.error_handling.exceptions;

public class ManifestListResolutionException extends BaseException {
  public ManifestListResolutionException(String message) {
    super(message);
  }

  public ManifestListResolutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
