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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class LogLeakFixturesTest {

  @Test
  void theUpstreamBodyEchoesTheSecret() {
    final var error = LogLeakFixtures.upstreamErrorEchoing("s3cr3t");

    assertThat(error.getStatusCode().value()).isEqualTo(400);
    assertThat(error.getResponseBodyAsString()).contains("s3cr3t");
  }

  @Test
  void theUnauthorizedErrorCarriesTheSecretInItsTextAndHeaders() {
    final var error = LogLeakFixtures.upstreamUnauthorizedWithSecretHeaders("s3cr3t");

    assertThat(error.getStatusCode().value()).isEqualTo(401);
    assertThat(error.getStatusText()).contains("s3cr3t");
    assertThat(error.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("s3cr3t");
    assertThat(error.getResponseHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE))
        .contains("s3cr3t");
  }
}
