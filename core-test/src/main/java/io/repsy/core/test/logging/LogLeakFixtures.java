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
package io.repsy.core.test.logging;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/** Failures whose messages and bodies embed a request value, for the log-leak tests. */
public final class LogLeakFixtures {

  private LogLeakFixtures() {}

  /**
   * A failed upstream call whose response body echoes {@code secret}.
   *
   * @param secret the value the body embeds
   * @return a 400 error with that body
   */
  public static HttpClientErrorException upstreamErrorEchoing(final String secret) {
    return HttpClientErrorException.create(
        HttpStatus.BAD_REQUEST,
        "Bad Request",
        new HttpHeaders(),
        ("{\"echo\":\"" + secret + "\"}").getBytes(UTF_8),
        UTF_8);
  }

  /**
   * A failed upstream call whose status text and response headers embed {@code secret}.
   *
   * @param secret the value the text and the headers embed
   * @return a 401 error with those headers
   */
  public static HttpClientErrorException upstreamUnauthorizedWithSecretHeaders(
      final String secret) {
    final var headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, "sid=" + secret);
    headers.add(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"" + secret + "\"");

    return HttpClientErrorException.create(
        HttpStatus.UNAUTHORIZED, "Unauthorized " + secret, headers, new byte[0], UTF_8);
  }
}
