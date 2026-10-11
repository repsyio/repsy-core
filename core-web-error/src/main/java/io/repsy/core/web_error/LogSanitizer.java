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

import java.util.ArrayList;
import java.util.Locale;

/**
 * Makes client-controlled text safe to log: URL-bearing header values are reduced to {@code
 * scheme://host[:port]}, control characters are escaped and anything that does not parse as a plain
 * host is replaced by {@link #REDACTED}. All methods are single-pass and regex-free.
 */
final class LogSanitizer {

  /** Written instead of a value that cannot be shown safely. */
  static final String REDACTED = "***";

  private static final int MAX_VALUE_LENGTH = 2048;
  private static final int MAX_NAME_LENGTH = 64;
  private static final int MAX_HOST_LENGTH = 253;
  private static final int MAX_PORT_LENGTH = 5;
  private static final int MAX_SCHEME_LENGTH = 16;
  private static final int MAX_FORWARDED_HOSTS = 8;
  private static final int MIN_LITERAL_LENGTH = 3;

  private LogSanitizer() {}

  /**
   * Reduces an {@code Origin} or {@code Referer} value to {@code scheme://host[:port]}. Userinfo,
   * path, query and fragment are dropped.
   *
   * @param value the raw header value
   * @return the origin, {@code null} for the literal {@code null} origin, or {@link #REDACTED}
   */
  static String origin(final String value) {

    final var text = value.strip();

    if ("null".equals(text)) {
      return text;
    }

    final var separator = schemeEnd(text);

    if (separator < 0) {
      return REDACTED;
    }

    final var authority = authority(text, separator + 3);
    final var host = hostAndPort(authority.substring(authority.lastIndexOf('@') + 1));

    if (host == null) {
      return REDACTED;
    }

    return text.substring(0, separator).toLowerCase(Locale.ROOT) + "://" + host;
  }

  /**
   * Validates an {@code X-Forwarded-Host} value: a comma separated list of {@code host[:port]}.
   *
   * @param value the raw header value
   * @return the hosts, or {@link #REDACTED}
   */
  static String forwardedHost(final String value) {

    if (value.length() > MAX_VALUE_LENGTH) {
      return REDACTED;
    }

    final var hosts = new ArrayList<String>();

    for (final var part : value.split(",", -1)) {
      final var host = hostAndPort(part.strip());

      if (host == null || hosts.size() == MAX_FORWARDED_HOSTS) {
        return REDACTED;
      }

      hosts.add(host);
    }

    return String.join(", ", hosts);
  }

  /**
   * Escapes control characters so a value cannot forge log lines, and caps its length.
   *
   * @param value the raw value
   * @return the value with every control character written as {@code \}{@code uNNNN}
   */
  static String escape(final String value) {

    final var out = new StringBuilder(Math.min(value.length(), MAX_VALUE_LENGTH));

    for (var i = 0; i < value.length() && i < MAX_VALUE_LENGTH; i++) {
      final var c = value.charAt(i);

      if (isLineOrControl(c)) {
        out.append(String.format("\\u%04x", (int) c));
      } else {
        out.append(c);
      }
    }

    return out.toString();
  }

  /**
   * Keeps a query parameter name only when it is short and made of {@code [A-Za-z0-9_.\-\[\]]}.
   *
   * @param name the raw name
   * @return the name or {@link #REDACTED}
   */
  static String parameterName(final String name) {

    if (name.length() > MAX_NAME_LENGTH) {
      return REDACTED;
    }

    for (var i = 0; i < name.length(); i++) {
      final var c = name.charAt(i);

      if (!isAlphaNumeric(c) && "_.-[]".indexOf(c) < 0) {
        return REDACTED;
      }
    }

    return name;
  }

  /** Returns the index of {@code ://} when the text starts with a valid scheme, otherwise -1. */
  private static int schemeEnd(final String text) {

    final var separator = text.indexOf("://");
    final var plausible =
        text.length() <= MAX_VALUE_LENGTH && separator > 0 && separator <= MAX_SCHEME_LENGTH;

    return plausible && isScheme(text, separator) ? separator : -1;
  }

  private static boolean isScheme(final String text, final int end) {

    for (var i = 0; i < end; i++) {
      final var c = text.charAt(i);
      final var ok = isAlpha(c) || i > 0 && (isDigit(c) || "+-.".indexOf(c) >= 0);

      if (!ok) {
        return false;
      }
    }

    return true;
  }

  private static String authority(final String text, final int start) {

    var end = start;

    while (end < text.length() && "/?#\\".indexOf(text.charAt(end)) < 0) {
      end++;
    }

    return text.substring(start, end);
  }

  /** Returns {@code host[:port]} when it is a plain host, otherwise {@code null}. */
  private static String hostAndPort(final String authority) {

    final var end = hostEnd(authority);

    if (end <= 0) {
      return null;
    }

    final var host = authority.substring(0, end);
    final var rest = authority.substring(end);

    return isHost(host) && isPortSuffix(rest) ? authority : null;
  }

  private static boolean isHost(final String host) {
    return host.startsWith("[") ? isIpLiteral(host) : isHostName(host);
  }

  private static boolean isPortSuffix(final String rest) {
    return rest.isEmpty() || rest.charAt(0) == ':' && isPort(rest, 1);
  }

  private static int hostEnd(final String authority) {

    if (authority.startsWith("[")) {
      return authority.indexOf(']') + 1;
    }

    final var colon = authority.indexOf(':');

    return colon < 0 ? authority.length() : colon;
  }

  private static boolean isHostName(final String host) {

    if (host.isEmpty() || host.length() > MAX_HOST_LENGTH) {
      return false;
    }

    for (var i = 0; i < host.length(); i++) {
      final var c = host.charAt(i);

      if (!isAlphaNumeric(c) && "-._".indexOf(c) < 0) {
        return false;
      }
    }

    return true;
  }

  private static boolean isLiteralShape(final String host) {
    return host.length() >= MIN_LITERAL_LENGTH
        && host.length() <= MAX_HOST_LENGTH
        && host.charAt(host.length() - 1) == ']';
  }

  private static boolean isIpLiteral(final String host) {

    if (!isLiteralShape(host)) {
      return false;
    }

    for (var i = 1; i < host.length() - 1; i++) {
      final var c = host.charAt(i);

      if (!isHex(c) && ":.".indexOf(c) < 0) {
        return false;
      }
    }

    return true;
  }

  private static boolean isPort(final String text, final int start) {

    final var length = text.length() - start;

    if (length < 1 || length > MAX_PORT_LENGTH) {
      return false;
    }

    for (var i = start; i < text.length(); i++) {
      if (!isDigit(text.charAt(i))) {
        return false;
      }
    }

    return true;
  }

  private static boolean isLineOrControl(final char c) {

    final var type = Character.getType(c);

    return Character.isISOControl(c)
        || type == Character.LINE_SEPARATOR
        || type == Character.PARAGRAPH_SEPARATOR;
  }

  private static boolean isAlpha(final char c) {
    return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
  }

  private static boolean isDigit(final char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isHex(final char c) {
    return isDigit(c) || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
  }

  private static boolean isAlphaNumeric(final char c) {
    return isAlpha(c) || isDigit(c);
  }
}
