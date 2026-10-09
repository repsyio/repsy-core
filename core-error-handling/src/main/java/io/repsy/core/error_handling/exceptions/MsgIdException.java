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
package io.repsy.core.error_handling.exceptions;

import org.jspecify.annotations.Nullable;

/**
 * An exception that carries a bare msgId (see {@link MsgIds}), which is the stable, client-visible
 * code of the failure.
 */
public abstract sealed class MsgIdException extends BaseException
    permits BadRequestException,
        UnAuthorizedException,
        AccessNotAllowedException,
        ItemNotFoundException,
        ItemAlreadyExistException,
        SubscriptionLimitReachedException,
        MfaException,
        SignatureNotVerifiedException,
        DataExportRequestException {

  private final int status;

  MsgIdException(final @Nullable String msgId, final int status) {
    super(MsgIds.require(msgId));
    this.status = status;
  }

  @Override
  public int status() {
    return this.status;
  }

  @Override
  public @Nullable String publicCode() {
    return getMessage();
  }
}
