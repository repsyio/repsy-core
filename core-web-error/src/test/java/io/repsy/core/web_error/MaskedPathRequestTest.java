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
package io.repsy.core.web_error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MaskedPathRequest hides token-bearing path segments")
class MaskedPathRequestTest {

  private static final String SECRET = "s3cr3t-token-value";

  @Test
  @DisplayName("masks the URI, the servlet path and the path info (RPS-2202)")
  void masksEveryPathAccessor() {
    final var request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/a/token/" + SECRET);
    when(request.getServletPath()).thenReturn("/a/token/" + SECRET);
    when(request.getPathInfo()).thenReturn("/token/" + SECRET);

    final var masked = MaskedPathRequest.of(request);

    assertThat(masked.getRequestURI()).isEqualTo("/a/token/***");
    assertThat(masked.getServletPath()).isEqualTo("/a/token/***");
    assertThat(masked.getPathInfo()).isEqualTo("/token/***");
    assertThat(MaskedPathRequest.of(masked)).isSameAs(masked);
  }

  @Test
  @DisplayName("passes a null path info through")
  void nullPathInfo() {
    final var masked = MaskedPathRequest.of(mock(HttpServletRequest.class));

    assertThat(masked.getPathInfo()).isNull();
  }
}
