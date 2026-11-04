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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Abuse guard of an anonymous {@code POST} endpoint: a per client address rate limit, then a cap on
 * the declared body size. It runs before the body is read; the exceptions it throws ({@link
 * TooManyRequestsException}, {@link PayloadTooLargeException}) are answered by the consuming
 * service. A body without a {@code Content-Length} (chunked) is refused, since its size cannot be
 * capped before it is parsed. Requests other than {@code POST} pass untouched.
 */
public class RequestGuardInterceptor implements HandlerInterceptor {

  private final ClientAddressRateLimiter rateLimiter;
  private final long maxPayloadBytes;

  /**
   * Creates the guard.
   *
   * @param rateLimiter the limiter that counts a request of the caller's address
   * @param maxPayloadBytes the largest declared body size that is accepted
   */
  public RequestGuardInterceptor(
      final ClientAddressRateLimiter rateLimiter, final long maxPayloadBytes) {
    this.rateLimiter = rateLimiter;
    this.maxPayloadBytes = maxPayloadBytes;
  }

  @Override
  public boolean preHandle(
      final HttpServletRequest request, final HttpServletResponse response, final Object handler) {

    if (!HttpMethod.POST.matches(request.getMethod())) {
      return true;
    }

    this.rateLimiter.check(request.getRemoteAddr());

    final var length = request.getContentLengthLong();
    if (length < 0 || length > this.maxPayloadBytes) {
      throw new PayloadTooLargeException();
    }

    return true;
  }
}
