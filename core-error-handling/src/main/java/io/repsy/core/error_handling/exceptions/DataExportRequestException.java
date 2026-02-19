package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.NonNull;

public class DataExportRequestException extends BaseException {
  public DataExportRequestException(final @NonNull String msgId) {
    super(msgId);
  }
}
