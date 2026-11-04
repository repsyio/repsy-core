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
package io.repsy.core.web.guard;

/** A caller exceeded a rate limit; the consuming service answers with 429 and a Retry-After. */
public class TooManyRequestsException extends RuntimeException {

  private final long retryAfterSeconds;

  public TooManyRequestsException(final long retryAfterSeconds) {
    super("Too many requests");
    this.retryAfterSeconds = retryAfterSeconds;
  }

  /**
   * The seconds until the caller's window ends.
   *
   * @return the value for the {@code Retry-After} header
   */
  public long getRetryAfterSeconds() {
    return this.retryAfterSeconds;
  }
}
