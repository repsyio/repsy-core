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

import java.util.Locale;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

/**
 * Makes upstream URLs, and texts that contain them, safe to log (RPS-2174). A proxy repository URL
 * can embed credentials as userinfo ({@code https://user:token@host/...}) or as query parameters,
 * and the messages of HTTP client exceptions repeat the URL they failed on.
 *
 * <p>The masking fails closed: HTTP clients disagree on where the userinfo of a malformed URL ends
 * ({@code user:p@ss@host}, {@code user:to#ken@host}, backslashes), so nothing of the authority is
 * kept that precedes the last {@code @} of the URL, and a URL is reduced to {@code
 * scheme://host[:port]}, followed by {@code /...} when a path, query or fragment was cut. An
 * authority that is not a plain host (and port) becomes {@code scheme://<redacted>}. A path with an
 * {@code @} in it (an npm scope) is shortened too, which is fine for a log. An {@code @} behind a
 * {@code /}, {@code ?}, {@code #}, backslash or a space is not trusted as the end of the userinfo
 * either: the authority is then {@code <redacted>}. The tokens that follow the URL and look like
 * URL content ({@code /}, {@code ?}, {@code #}, {@code =}, {@code &}, {@code %}, {@code @}) are
 * dropped with it, plain words in between included, up to the end of the line: prose cannot be told
 * from a path with a space in it.
 *
 * <p>Every control character of the result is replaced by a visible escape so that an exception
 * message cannot forge log lines. The scan is linear in the length of the text.
 */
// A hand-written linear scan (no regular expression, so no backtracking on hostile input): the
// branching is the character classification, which does not split into meaningful methods.
@SuppressWarnings({"checkstyle:CyclomaticComplexity", "checkstyle:MagicNumber"})
@UtilityClass
public class UrlMasking {

  /**
   * Path segments after which the next segment is a secret (an npm token revoked by {@code DELETE
   * /-/user/token/<token>}, a signed or download link, a coupon code).
   */
  private static final Set<String> SECRET_MARKERS =
      Set.of(
          "token",
          "download-token",
          "secret",
          "secrets",
          "signature",
          "signed",
          "otp",
          "coupon",
          "coupons",
          "unsubscribe");

  private static final String MASKED_SEGMENT = "***";
  private static final int MAX_PATH_LENGTH = 512;
  private static final String SEPARATOR = "://";
  private static final String REDACTED = "<redacted>";
  private static final String CUT = "/...";
  private static final int MAX_HOST_LENGTH = 255;

  /** Returns the text with every URL in it masked and control characters escaped. */
  public static @Nullable String mask(final @Nullable String text) {
    final var masked = maskUrls(text);

    return masked == null ? null : escapeControlCharacters(masked);
  }

  /**
   * A request path made safe to log: every segment that follows a secret-bearing one ({@code
   * /token/<secret>}) is replaced, because the container normalises {@code .}, {@code ..}, empty
   * and {@code ;param} segments before routing and the secret need not be the next literal segment.
   * URLs and control characters are handled as in {@link #mask(String)}, and the result is cut to a
   * sane length. The query string is not part of a path and is never expected here.
   */
  public static @Nullable String maskPath(final @Nullable String path) {
    if (path == null || path.isEmpty()) {
      return path;
    }

    final var segments = path.split("/", -1);

    // Fail closed: the container normalises the path before routing ('.', '..', empty and
    // ';params' segments), so the secret is not always the literal next segment. Everything after
    // a marker is masked.
    boolean afterMarker = false;

    for (int i = 0; i < segments.length; i++) {
      if (afterMarker) {
        segments[i] = MASKED_SEGMENT;
      } else if (SECRET_MARKERS.contains(markerForm(segments[i]))) {
        afterMarker = true;
      }
    }

    final var joined = String.join("/", segments);
    final var cut =
        joined.length() > MAX_PATH_LENGTH ? joined.substring(0, MAX_PATH_LENGTH) : joined;

    return mask(cut);
  }

  /**
   * The form of a path segment a router would compare against a secret marker: without its path
   * parameters ({@code token;x=1}), percent-decoded (twice at most, so a double-encoded marker does
   * not slip through) and lower-cased. The logged path is the raw request URI, while the route that
   * served the request matched the decoded one.
   */
  private static String markerForm(final String segment) {
    var form = segment;
    final int parameters = form.indexOf(';');

    if (parameters >= 0) {
      form = form.substring(0, parameters);
    }

    for (int pass = 0; pass < 2 && form.indexOf('%') >= 0; pass++) {
      form = decodeAsciiPercent(form);
    }

    return form.toLowerCase(Locale.ROOT);
  }

  /** Decodes {@code %HH} sequences that stand for an ASCII character, leaves any other as is. */
  private static String decodeAsciiPercent(final String value) {
    final var out = new StringBuilder(value.length());
    int i = 0;

    while (i < value.length()) {
      final int decoded = asciiPercentAt(value, i);

      if (decoded >= 0) {
        out.append((char) decoded);
        i += 3;
      } else {
        out.append(value.charAt(i));
        i++;
      }
    }

    return out.toString();
  }

  /** The ASCII character the {@code %HH} sequence at {@code index} stands for, or -1 if none. */
  private static int asciiPercentAt(final String value, final int index) {
    if (value.charAt(index) != '%'
        || index + 2 >= value.length()
        || !isHexDigit(value.charAt(index + 1))
        || !isHexDigit(value.charAt(index + 2))) {
      return -1;
    }

    final int decoded = Integer.parseInt(value.substring(index + 1, index + 3), 16);

    return decoded < 0x80 ? decoded : -1;
  }

  /**
   * Returns the text with every URL in it masked; line breaks stay, for a sink that renders
   * multi-line text (the Slack message, with its stack trace).
   */
  public static @Nullable String maskUrls(final @Nullable String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }

    final var out = new StringBuilder(text.length());
    int copied = 0;
    int from = 0;
    int sep = text.indexOf(SEPARATOR, from);

    while (sep >= 0) {
      if (hasScheme(text, copied, sep)) {
        copied = appendMaskedUrl(out, text, copied, sep);
        from = copied;
      } else {
        from = sep + SEPARATOR.length();
      }

      sep = text.indexOf(SEPARATOR, from);
    }

    out.append(text, copied, text.length());

    return out.toString();
  }

  /**
   * Whether the characters before the separator at {@code sep}, from {@code copied} on, end in a
   * scheme.
   */
  private static boolean hasScheme(final String text, final int copied, final int sep) {
    return schemeStart(text, copied, sep) >= 0;
  }

  /**
   * The index where the scheme that ends at the separator {@code sep} starts, or -1 if there is
   * none. A scheme starts with an ASCII letter ({@code 1://} and {@code +://} are not schemes). The
   * main loop and {@link #regionEnd} must agree on this, or the text after a separator that one of
   * them skips would be copied as it is while the other considers it part of a URL.
   */
  private static int schemeStart(final String text, final int lowerBound, final int sep) {
    int start = sep;

    while (start > lowerBound && isSchemeChar(text.charAt(start - 1))) {
      start--;
    }

    while (start < sep && !isAsciiLetter(text.charAt(start))) {
      start++;
    }

    return start < sep ? start : -1;
  }

  /**
   * Appends the text up to the URL whose separator is at {@code sep}, then the masked URL, and
   * returns where the URL token ends.
   */
  private static int appendMaskedUrl(
      final StringBuilder out, final String text, final int copied, final int sep) {
    final int start = sep + SEPARATOR.length();
    final int end = regionEnd(text, start, text.length());

    // The authority starts after the LAST '@' anywhere in the region, whatever character (space,
    // '#', '/', '?', a quote) precedes it: parsers disagree where the userinfo ends.
    final int at = lastAtSign(text, start, end);
    final int authority = at >= 0 ? at + 1 : start;
    final int hostEnd = firstAuthorityDelimiter(text, authority, end);
    final int tokenEnd = firstTokenEnd(text, hostEnd, end);
    final int urlEnd = skipUrlContent(text, tokenEnd, end);

    out.append(text, copied, sep).append(SEPARATOR);

    // An '@' behind a delimiter ('/', '?', '#', a space...) may be in the path or the query, not
    // in the authority: what follows it is then not known to be the host.
    if (isHostAndPort(text, authority, hostEnd)
        && !hasDelimiter(text, start, Math.max(at, start))) {
      out.append(text, authority, hostEnd);
    } else {
      out.append(REDACTED);
    }

    if (at >= 0 || urlEnd > hostEnd) {
      out.append(CUT);
    }

    return urlEnd;
  }

  /**
   * The end of what still belongs to the URL after its token ended: everything up to the last token
   * on the same line that looks like URL content ({@code /}, {@code ?}, {@code #}, {@code =},
   * {@code &}, {@code %}, {@code @}, {@code \}), plain words in between included. An unencoded
   * space in the path or a quote closing before the query does not end the URL for every client,
   * and prose cannot be told from a path with a space in it, so this fails closed. A line break
   * ends it, and so does a line without such a token, so the prose after a URL is kept.
   */
  private static int skipUrlContent(final String text, final int from, final int end) {
    int kept = from;
    int i = from;

    while (i < end) {
      while (i < end && isContinuationGap(text.charAt(i))) {
        i++;
      }

      if (i >= end || isLineBreak(text.charAt(i))) {
        break;
      }

      boolean urlContent = false;

      while (i < end && !isSpace(text.charAt(i))) {
        urlContent |= isUrlContent(text.charAt(i));
        i++;
      }

      if (urlContent) {
        kept = i;
      }
    }

    return kept;
  }

  /** Whether any character in {@code [from, to)} ends the authority. */
  private static boolean hasDelimiter(final String text, final int from, final int to) {
    for (int i = from; i < to; i++) {
      if (isAuthorityDelimiter(text.charAt(i))) {
        return true;
      }
    }

    return false;
  }

  /** The index of the last '@' in {@code [start, end)}, or -1; never scans outside the region. */
  private static int lastAtSign(final String text, final int start, final int end) {
    int i = end - 1;

    while (i >= start && text.charAt(i) != '@') {
      i--;
    }

    return i >= start ? i : -1;
  }

  private static int firstAuthorityDelimiter(final String text, final int from, final int end) {
    int i = from;

    while (i < end && !isAuthorityDelimiter(text.charAt(i))) {
      i++;
    }

    return i;
  }

  private static int firstTokenEnd(final String text, final int from, final int end) {
    int i = from;

    while (i < end && !isTokenEnd(text.charAt(i))) {
      i++;
    }

    return i;
  }

  /**
   * The end of the part of the text that may belong to the URL starting at {@code start}: the
   * scheme of the next URL, or the end of the text. Whitespace and quotes do not end it, since a
   * space or quote inside userinfo must not let the part before it escape the masking.
   */
  private static int regionEnd(final String text, final int start, final int length) {
    for (int i = start; i < length; i++) {
      if (text.charAt(i) == ':' && text.startsWith(SEPARATOR, i)) {
        final int scheme = schemeStart(text, start, i);

        if (scheme >= 0) {
          return scheme;
        }
      }
    }

    return length;
  }

  private static boolean isTokenEnd(final char c) {
    return c == '"' || c == '\'' || c == '<' || c == '>' || Character.isWhitespace(c) || c < 0x20;
  }

  /** What separates tokens on a line: any space or control character, and the URL delimiters. */
  private static boolean isContinuationGap(final char c) {
    return (isSpace(c) && !isLineBreak(c)) || c == '"' || c == '\'' || c == '<' || c == '>';
  }

  /** Every kind of white space and control character, so no Unicode space hides a token. */
  private static boolean isSpace(final char c) {
    return Character.isWhitespace(c) || Character.isSpaceChar(c) || c < 0x20 || c == 0x7f;
  }

  private static boolean isLineBreak(final char c) {
    return c == '\n' || c == '\r' || c == 0x85 || c == 0x2028 || c == 0x2029;
  }

  private static boolean isUrlContent(final char c) {
    return c == '/' || c == '?' || c == '#' || c == '=' || c == '&' || c == '%' || c == '@'
        || c == '\\';
  }

  private static boolean isAuthorityDelimiter(final char c) {
    return c == '/' || c == '?' || c == '#' || c == '\\' || isTokenEnd(c);
  }

  /** A DNS name, IPv4 address or bracketed IPv6 literal, optionally followed by {@code :port}. */
  private static boolean isHostAndPort(final String text, final int from, final int to) {
    if (from >= to || to - from > MAX_HOST_LENGTH) {
      return false;
    }

    final int hostEnd =
        text.charAt(from) == '[' ? ipv6LiteralEnd(text, from, to) : dnsNameEnd(text, from, to);

    return hostEnd >= 0 && isOptionalPort(text, hostEnd, to);
  }

  /**
   * The index after the bracketed IPv6 literal starting at {@code from}, or -1 if it is not one.
   */
  private static int ipv6LiteralEnd(final String text, final int from, final int to) {
    final int close = text.indexOf(']', from);

    if (close < 0 || close >= to || close == from + 1) {
      return -1;
    }

    for (int j = from + 1; j < close; j++) {
      final char c = text.charAt(j);

      if (!(isHexDigit(c) || c == ':' || c == '.')) {
        return -1;
      }
    }

    return close + 1;
  }

  /** The index after the DNS name or IPv4 address starting at {@code from}, or -1 if none. */
  private static int dnsNameEnd(final String text, final int from, final int to) {
    int i = from;

    while (i < to && text.charAt(i) != ':') {
      final char c = text.charAt(i);

      if (!(isAsciiLetter(c) || isDigit(c) || c == '.' || c == '-' || c == '_')) {
        return -1;
      }

      i++;
    }

    return i == from ? -1 : i;
  }

  /** Whether {@code hostEnd} is the end, or is followed only by {@code :port}. */
  private static boolean isOptionalPort(final String text, final int hostEnd, final int to) {
    if (hostEnd == to) {
      return true;
    }

    if (text.charAt(hostEnd) != ':' || to - hostEnd - 1 > 5 || to - hostEnd == 1) {
      return false;
    }

    for (int j = hostEnd + 1; j < to; j++) {
      if (!isDigit(text.charAt(j))) {
        return false;
      }
    }

    return true;
  }

  private static String escapeControlCharacters(final CharSequence value) {
    final var result = new StringBuilder(value.length());

    for (int i = 0; i < value.length(); i++) {
      final char c = value.charAt(i);

      if (c == '\r') {
        result.append("\\r");
      } else if (c == '\n') {
        result.append("\\n");
      } else if (c == '\t') {
        result.append("\\t");
      } else if (c < 0x20 || c == 0x7f || (c >= 0x80 && c <= 0x9f) || c == 0x2028 || c == 0x2029) {
        result.append(String.format("\\u%04x", (int) c));
      } else {
        result.append(c);
      }
    }

    return result.toString();
  }

  private static boolean isSchemeChar(final char c) {
    return isAsciiLetter(c) || isDigit(c) || c == '+' || c == '.' || c == '-';
  }

  private static boolean isAsciiLetter(final char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }

  private static boolean isDigit(final char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isHexDigit(final char c) {
    return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }
}
