package io.repsy.core.response.services;

import io.repsy.core.response.dtos.ResponseType;
import io.repsy.core.response.dtos.RestResponse;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class RestResponseFactory {
  private final @NonNull MessageSource messageSource;

  public <T> @NonNull RestResponse<T> error(final @NonNull String msgId) {
    return this.createMessage(ResponseType.ERROR, msgId);
  }

  public <T> @NonNull RestResponse<T> error(final @NonNull String msgId, final T data) {
    return this.createMessage(ResponseType.ERROR, msgId, data);
  }

  public <T> @NonNull RestResponse<T> success(final @NonNull String msgId) {
    return this.createMessage(ResponseType.SUCCESS, msgId);
  }

  public <T> @NonNull RestResponse<T> success(final @NonNull String msgId, final T data) {
    return this.createMessage(ResponseType.SUCCESS, msgId, data);
  }

  public <T> @NonNull RestResponse<T> warning(final @NonNull String msgId) {
    return this.createMessage(ResponseType.WARNING, msgId);
  }

  public <T> @NonNull RestResponse<T> warning(final @NonNull String msgId, final @Nullable T data) {
    return this.createMessage(ResponseType.WARNING, msgId, data);
  }

  public RestResponseFactory(final @NonNull MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  private @NonNull <T> RestResponse<T> createMessage(
      final @NonNull ResponseType type, final @NonNull String msgId) {
    return this.createMessage(type, msgId, null);
  }

  private @NonNull <T> RestResponse<T> createMessage(
      final @NonNull ResponseType type, final @NonNull String msgId, final @Nullable T data) {
    final RestResponse<T> restResponse = new RestResponse<>(msgId, type);

    if (type == ResponseType.ERROR) {
      restResponse.setErrorCode(UUID.randomUUID().toString());
    }

    restResponse.setText(
        Objects.requireNonNull(
            this.messageSource.getMessage(msgId, null, msgId, Locale.getDefault())));
    restResponse.setData(data);

    return restResponse;
  }
}
