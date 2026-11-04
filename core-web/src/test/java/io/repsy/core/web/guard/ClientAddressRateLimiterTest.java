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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ClientAddressRateLimiterTest {

  private final AtomicReference<Instant> now =
      new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));

  private final Clock clock =
      new Clock() {
        @Override
        public java.time.ZoneId getZone() {
          return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
          return this;
        }

        @Override
        public Instant instant() {
          return now.get();
        }
      };

  @Test
  void refusesTheRequestOverTheLimitWithTheRemainingWindow() {
    final var limiter = new ClientAddressRateLimiter(clock, 2, 60, 100);

    limiter.check("1.1.1.1");
    limiter.check("1.1.1.1");
    now.set(now.get().plusSeconds(10));

    assertThatThrownBy(() -> limiter.check("1.1.1.1"))
        .isInstanceOfSatisfying(
            TooManyRequestsException.class,
            e -> assertThat(e.getRetryAfterSeconds()).isEqualTo(50));
  }

  @Test
  void countsEachAddressSeparately() {
    final var limiter = new ClientAddressRateLimiter(clock, 1, 60, 100);

    limiter.check("1.1.1.1");

    assertThatCode(() -> limiter.check("2.2.2.2")).doesNotThrowAnyException();
  }

  @Test
  void startsANewWindowWhenTheOldOneExpired() {
    final var limiter = new ClientAddressRateLimiter(clock, 1, 60, 100);

    limiter.check("1.1.1.1");
    now.set(now.get().plus(Duration.ofSeconds(60)));

    assertThatCode(() -> limiter.check("1.1.1.1")).doesNotThrowAnyException();
  }

  @Test
  void neverRefusesANewClientOnlyBecauseTheMapIsFull() {
    final var limiter = new ClientAddressRateLimiter(clock, 1, 60, 3);

    for (var i = 1; i <= 4; i++) {
      final var address = "10.0.0." + i;
      assertThatCode(() -> limiter.check(address)).doesNotThrowAnyException();
    }
  }

  @Test
  void aLimitedKeyStaysLimitedWhileFewerThanNOtherKeysChurn() {
    final var limiter = new ClientAddressRateLimiter(clock, 1, 60, 4);
    limiter.check("10.0.0.1");

    for (var i = 2; i <= 4; i++) {
      limiter.check("10.0.0." + i);
      assertThatThrownBy(() -> limiter.check("10.0.0.1"))
          .isInstanceOf(TooManyRequestsException.class);
    }
  }

  @Test
  void staysBoundedWhenManyAddressesArrive() {
    final var limiter = new ClientAddressRateLimiter(clock, 1, 60, 3);

    for (var i = 0; i < 50; i++) {
      limiter.check("10.0.0." + i);
    }

    assertThat(limiter.trackedClients()).isLessThanOrEqualTo(3);
  }

  @Test
  void dropsExpiredWindowsLazily() {
    final var limiter = new ClientAddressRateLimiter(clock, 1, 60, 2);
    limiter.check("10.0.0.1");
    now.set(now.get().plusSeconds(61));

    assertThatCode(() -> limiter.check("10.0.0.1")).doesNotThrowAnyException();
  }

  @Test
  void sharesTheCounterOfOneIpv6Prefix() {
    final var limiter = new ClientAddressRateLimiter(clock, 1, 60, 100);
    limiter.check("2001:db8:1:2:aaaa:bbbb:cccc:dddd");

    assertThatThrownBy(() -> limiter.check("2001:db8:1:2:1111:2222:3333:4444"))
        .isInstanceOf(TooManyRequestsException.class);
    assertThatCode(() -> limiter.check("2001:db8:1:3::1")).doesNotThrowAnyException();
  }

  @Test
  void keysAnIpv4MappedAddressAsIpv4() {
    assertThat(ClientAddressRateLimiter.clientKey("::ffff:192.0.2.7")).isEqualTo("192.0.2.7");
    assertThat(ClientAddressRateLimiter.clientKey("192.0.2.7")).isEqualTo("192.0.2.7");
    assertThat(ClientAddressRateLimiter.clientKey("not an address")).isEqualTo("not an address");
    assertThat(ClientAddressRateLimiter.clientKey("example.com:80")).isEqualTo("example.com:80");
  }

  @Test
  void keysAMalformedIpv6LiteralAsTheRawStringWithoutALookup() {
    assertThat(ClientAddressRateLimiter.clientKey("1:2:3")).isEqualTo("1:2:3");
    assertThat(ClientAddressRateLimiter.clientKey("abcd:ef::g")).isEqualTo("abcd:ef::g");
    assertThat(ClientAddressRateLimiter.clientKey("2001:db8::1%eth0"))
        .isEqualTo("20010db800000000::/64");
  }
}
