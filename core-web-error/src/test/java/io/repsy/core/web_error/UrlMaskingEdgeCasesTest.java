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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Edge cases of {@link UrlMasking} that {@link UrlMaskingTest} does not name (moved from the repsy
 * OS repository, RPS-2519): authorities that are not a plain host, accepted host and scheme
 * characters, URL ends, control characters and percent sequences.
 */
@DisplayName("UrlMasking edge cases: odd authorities, schemes, control characters and escapes")
class UrlMaskingEdgeCasesTest {

  private static final String SECRET = "s3cr3t-token-value";

  // Anything that is not a plain host (and port) is not echoed: it may be a secret.
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "https://[]/x|https://<redacted>/...",
        "https://[::1/x|https://<redacted>/...",
        "https://[zz]/x|https://<redacted>/...",
        "https://[::1]:123456/x|https://<redacted>/...",
        "https://[::1]:80/x|https://[::1]:80/...",
        "https://host:/x|https://<redacted>/...",
        "https://host:80a/x|https://<redacted>/...",
        "https://host:8080/x|https://host:8080/...",
        "https://:80/x|https://<redacted>/...",
        "https://ho$t/x|https://<redacted>/...",
        "https:///x|https://<redacted>/..."
      })
  void anAuthorityThatIsNotAPlainHostIsRedacted(final String input, final String expected) {
    assertThat(UrlMasking.mask(input)).isEqualTo(expected);
  }

  // The accepted characters of a host, an IPv6 literal and a scheme, and the ends of a URL.
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "https://my_host-1.example.com/p|https://my_host-1.example.com/...",
        "https://[::FFFF:1.2.3.4]/p|https://[::FFFF:1.2.3.4]/...",
        "https://[::1/]x|https://<redacted>/...",
        "git+ssh://h/x|git+ssh://h/...",
        "a-b.c://h/x|a-b.c://h/...",
        "https://h?x=1|https://h/...",
        "https://h#frag|https://h/...",
        "see <https://h/x> and 'https://h2/y' ok|see <https://h/...> and 'https://h2/...' ok",
        "\"https://h/x\" and (https://h2:80)|\"https://h/...\" and (https://<redacted>"
      })
  void hostSchemeAndUrlEndsAreRecognised(final String input, final String expected) {
    assertThat(UrlMasking.mask(input)).isEqualTo(expected);
  }

  @Test
  void aHostLongerThanADnsNameIsRedacted() {
    assertThat(UrlMasking.mask("https://" + "a".repeat(256) + "/x"))
        .isEqualTo("https://<redacted>/...");
  }

  // The scheme starts at the first letter: digits and signs in front of it are not part of it.
  @ParameterizedTest
  @CsvSource({
    "1http://host/x, 1http://host/...",
    "+-http://host/x, +-http://host/...",
    "123://host/x, 123://host/x",
    "://host/x, ://host/x"
  })
  void theSchemeStartsAtTheFirstLetter(final String input, final String expected) {
    assertThat(UrlMasking.mask(input)).isEqualTo(expected);
  }

  @Test
  void maskUrlsKeepsLineBreaksAndNullAndEmpty() {
    assertThat(UrlMasking.maskUrls(null)).isNull();
    assertThat(UrlMasking.maskUrls("")).isEmpty();
    assertThat(UrlMasking.maskUrls("x\nhttps://u:" + SECRET + "@h/p\ny"))
        .isEqualTo("x\nhttps://h/...\ny");
  }

  @Test
  void rareControlCharactersAreEscaped() {
    assertThat(UrlMasking.mask("a\u007fb\u0085c\u2029d\u009fe"))
        .isEqualTo("a\\u007fb\\u0085c\\u2029d\\u009fe");
    assertThat(UrlMasking.mask("caf\u00e9 \u00a0")).isEqualTo("caf\u00e9 \u00a0");
  }

  // Only %HH that stands for an ASCII character is decoded (to find the marker); the rest stays.
  @ParameterizedTest
  @CsvSource({
    "/token/%C3%A9/s3cr3t-token-value, /token/***/***",
    "/%C3token/x, /%C3token/x",
    "/x%4, /x%4",
    "/x%, /x%"
  })
  void nonAsciiAndTruncatedPercentSequencesAreLeftAlone(final String path, final String expected) {
    assertThat(UrlMasking.maskPath(path)).isEqualTo(expected);
  }

  @Test
  void maskPathKeepsEmptyPaths() {
    assertThat(UrlMasking.maskPath("")).isEmpty();
  }
}
