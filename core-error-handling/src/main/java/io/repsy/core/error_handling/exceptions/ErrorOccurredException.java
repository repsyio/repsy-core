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

/**
 * Wraps an unexpected failure. The response always carries the fixed {@code errorOccurred} msgId:
 * {@code ErrorHandler} returns the exception message as both the {@code msgId} and the {@code text}
 * of the response, so a constructor that accepted a free-text message would let a caller leak it
 * (or request content) to the client (RPS-1062).
 */
public class ErrorOccurredException extends BaseException {
  public ErrorOccurredException(final Exception ex) {
    super("errorOccurred", ex);
  }
}
