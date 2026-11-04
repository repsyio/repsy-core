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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestGuardInterceptorTest {

  private static final int MAX_PAYLOAD = 100;

  private final ClientAddressRateLimiter limiter = new ClientAddressRateLimiter(2, 60, 100);
  private final RequestGuardInterceptor guard = new RequestGuardInterceptor(limiter, MAX_PAYLOAD);
  private final MockHttpServletResponse response = new MockHttpServletResponse();

  private static MockHttpServletRequest post(final int contentLength) {
    final var request = new MockHttpServletRequest("POST", "/public");
    request.setRemoteAddr("1.1.1.1");
    request.setContent(new byte[contentLength]);
    return request;
  }

  @Test
  void letsARequestWithinTheLimitsThrough() {
    assertThat(guard.preHandle(post(MAX_PAYLOAD), response, new Object())).isTrue();
  }

  @Test
  void leavesOtherMethodsUntouchedAndUncounted() {
    final var get = new MockHttpServletRequest("GET", "/public");
    get.setRemoteAddr("1.1.1.1");

    for (var i = 0; i < 5; i++) {
      assertThat(guard.preHandle(get, response, new Object())).isTrue();
    }
    assertThat(limiter.trackedClients()).isZero();
  }

  @Test
  void refusesTheRequestOverTheRateLimit() {
    guard.preHandle(post(1), response, new Object());
    guard.preHandle(post(1), response, new Object());

    assertThatThrownBy(() -> guard.preHandle(post(1), response, new Object()))
        .isInstanceOf(TooManyRequestsException.class);
  }

  @Test
  void refusesADeclaredBodyAboveTheCap() {
    assertThatThrownBy(() -> guard.preHandle(post(MAX_PAYLOAD + 1), response, new Object()))
        .isInstanceOf(PayloadTooLargeException.class)
        .hasMessage("Payload too large");
  }

  @Test
  void refusesABodyWithoutADeclaredLength() {
    final var chunked = post(1);
    chunked.setContent(null);
    chunked.addHeader("Transfer-Encoding", "chunked");
    assertThatThrownBy(() -> guard.preHandle(chunked, response, new Object()))
        .isInstanceOf(PayloadTooLargeException.class);
  }
}
