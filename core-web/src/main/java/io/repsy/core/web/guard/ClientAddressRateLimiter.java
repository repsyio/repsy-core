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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Per-client-address fixed-window rate limiter for an anonymous endpoint. In memory and per
 * instance: it bounds abuse from one address, it is not a global quota. The map is bounded by least
 * recently used eviction: expired windows are dropped lazily when their key is seen again, and the
 * least recently used key is evicted when the map is full, so a flood of distinct addresses cannot
 * grow the heap and never locks a new client out. An attacker who rotates through at least {@code
 * maxTrackedClients} distinct keys within one window can age out its own counter; that is accepted
 * for the endpoints this guards. IPv6 addresses are keyed by their /64 prefix, since one subscriber
 * owns a whole /64.
 */
public class ClientAddressRateLimiter {

  private static final long MILLIS_PER_SECOND = 1000;
  private static final int PREFIX_BYTES = 8;
  private static final int INITIAL_CAPACITY = 16;
  private static final float LOAD_FACTOR = 0.75f;
  private static final Pattern IPV6_LITERAL = Pattern.compile("[0-9A-Fa-f:.]+");

  private record Window(long startedAtMillis, int count) {}

  private final Clock clock;
  private final int maxRequests;
  private final long windowMillis;
  private final int maxTrackedClients;
  private final Map<String, Window> windows;

  /**
   * Creates a limiter on the system clock.
   *
   * @param maxRequests requests one client may make in a window
   * @param windowSeconds length of a window
   * @param maxTrackedClients clients kept at once; the least recently used one is evicted beyond it
   */
  public ClientAddressRateLimiter(
      final int maxRequests, final long windowSeconds, final int maxTrackedClients) {
    this(Clock.systemUTC(), maxRequests, windowSeconds, maxTrackedClients);
  }

  ClientAddressRateLimiter(
      final Clock clock,
      final int maxRequests,
      final long windowSeconds,
      final int maxTrackedClients) {
    this.clock = clock;
    this.maxRequests = maxRequests;
    this.windowMillis = windowSeconds * MILLIS_PER_SECOND;
    this.maxTrackedClients = maxTrackedClients;
    this.windows =
        new LinkedHashMap<>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
          @Override
          protected boolean removeEldestEntry(final Map.Entry<String, Window> eldest) {
            return size() > ClientAddressRateLimiter.this.maxTrackedClients;
          }
        };
  }

  /**
   * Counts one request of {@code clientAddress}.
   *
   * @param clientAddress the address of the caller
   * @throws TooManyRequestsException when the caller is over the limit
   */
  public synchronized void check(final String clientAddress) {
    final var key = clientKey(clientAddress);
    final var now = this.clock.millis();
    var window = this.windows.get(key);

    if (window == null || now - window.startedAtMillis() >= this.windowMillis) {
      window = new Window(now, 0);
    }

    if (window.count() >= this.maxRequests) {
      final var remainingMillis = this.windowMillis - (now - window.startedAtMillis());
      throw new TooManyRequestsException(
          Math.max(1, (remainingMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND));
    }

    this.windows.put(key, new Window(window.startedAtMillis(), window.count() + 1));
  }

  synchronized int trackedClients() {
    return this.windows.size();
  }

  /**
   * The key of an address: IPv4 as is, IPv6 as its /64 prefix, IPv4-mapped IPv6 as the IPv4
   * address, anything unparsable as the raw string. Literal parsing only, no name lookup.
   */
  static String clientKey(final String address) {
    final var zoneAt = address.indexOf('%');
    final var literal = zoneAt >= 0 ? address.substring(0, zoneAt) : address;

    if (!isIpv6Literal(literal)) {
      return address;
    }

    try {
      return keyOf(InetAddress.getByName(literal));
    } catch (final UnknownHostException e) {
      return address;
    }
  }

  private static boolean isIpv6Literal(final String value) {
    return value.indexOf(':') >= 0 && IPV6_LITERAL.matcher(value).matches();
  }

  private static String keyOf(final InetAddress parsed) {
    if (parsed instanceof Inet4Address) {
      return parsed.getHostAddress();
    }
    final var bytes = parsed.getAddress();
    final var prefix = new StringBuilder();
    for (var i = 0; i < PREFIX_BYTES; i++) {
      prefix.append(String.format("%02x", bytes[i]));
    }
    return prefix + "::/64";
  }
}
