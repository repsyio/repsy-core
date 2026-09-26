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

import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * The rule for the message id an exception carries: a bare identifier that names an entry of the
 * consuming service's message bundle.
 *
 * <p>{@code ErrorHandler} returns the message of these exceptions as both the {@code msgId} and the
 * {@code text} of the response, so free text (a sentence, a request path, a value) would reach the
 * client as it is. The exception constructors reject it up front (RPS-1127).
 */
final class MsgIds {

  private static final Pattern BARE_IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+");

  private MsgIds() {}

  /**
   * Returns the id as it is when it is a bare identifier or {@code null} (no id: the error handler
   * answers with its default for the exception type).
   *
   * @param msgId The message id passed to an exception constructor
   * @return {@code msgId}
   * @throws IllegalArgumentException when the id is not made of letters, digits and underscores
   */
  static @Nullable String require(final @Nullable String msgId) {
    if (msgId != null && !BARE_IDENTIFIER.matcher(msgId).matches()) {
      throw new IllegalArgumentException(
          "A msgId must be a bare identifier ("
              + BARE_IDENTIFIER.pattern()
              + ") with an entry in messages.properties, not free text; log the details instead");
    }

    return msgId;
  }
}
