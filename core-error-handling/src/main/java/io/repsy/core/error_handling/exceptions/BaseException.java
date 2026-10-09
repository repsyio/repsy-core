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
 * Root of the exceptions an {@code ErrorHandler} maps to a response. It is sealed into the two
 * contracts: {@link MsgIdException} (a stable, client-visible code) and {@link TechnicalException}
 * (an internal failure whose message is never exposed).
 */
public abstract sealed class BaseException extends RuntimeException
    permits MsgIdException, TechnicalException {

  BaseException(final @Nullable String message) {
    super(message);
  }

  BaseException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * The HTTP status the exception maps to, as a plain number so this module needs no Spring web
   * dependency.
   *
   * @return the HTTP status code
   */
  public abstract int status();

  /**
   * The machine readable code that is safe to put in a response, or {@code null} when the exception
   * carries none and the handler answers with its default for the type. Never derived from free
   * text.
   *
   * @return the public code
   */
  public abstract @Nullable String publicCode();
}
