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
package io.repsy.core.response.dtos;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

public class RestResponse<T> {
  private final String msgId;
  @Getter private final ResponseType type;
  @Getter private @Nullable T data;
  @Getter private @Nullable String errorCode;
  @Getter private @Nullable String text;

  public RestResponse(final String msgId, final ResponseType type) {
    this.msgId = msgId;
    this.type = type;
  }

  public void setData(final @Nullable T data) {
    this.data = data;
  }

  public void setErrorCode(final String errorCode) {
    this.errorCode = errorCode;
  }

  public void setText(final String text) {
    this.text = text;
  }

  public String getMsgId() {
    return this.msgId;
  }
}
