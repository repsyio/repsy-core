/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.repsy.core.response.services;

import io.repsy.core.response.dtos.ResponseType;
import io.repsy.core.response.dtos.RestResponse;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class RestResponseFactory {
  private final MessageSource messageSource;

  public <T> RestResponse<T> error(final String msgId) {
    return this.createMessage(ResponseType.ERROR, msgId);
  }

  public <T> RestResponse<T> error(final String msgId, final T data) {
    return this.createMessage(ResponseType.ERROR, msgId, data);
  }

  public <T> RestResponse<T> success(final String msgId) {
    return this.createMessage(ResponseType.SUCCESS, msgId);
  }

  public <T> RestResponse<T> success(final String msgId, final T data) {
    return this.createMessage(ResponseType.SUCCESS, msgId, data);
  }

  public <T> RestResponse<T> warning(final String msgId) {
    return this.createMessage(ResponseType.WARNING, msgId);
  }

  public <T> RestResponse<T> warning(final String msgId, final @Nullable T data) {
    return this.createMessage(ResponseType.WARNING, msgId, data);
  }

  public RestResponseFactory(final MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  private <T> RestResponse<T> createMessage(final ResponseType type, final String msgId) {
    return this.createMessage(type, msgId, null);
  }

  private <T> RestResponse<T> createMessage(
      final ResponseType type, final String msgId, final @Nullable T data) {
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
