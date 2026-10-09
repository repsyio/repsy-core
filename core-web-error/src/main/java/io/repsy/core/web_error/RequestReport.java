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

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A loggable, secret-free snapshot of an HTTP request.
 *
 * <p>Only the path, the method, the names of the query parameters and the headers on an allow-list
 * are reported. Every other header is listed by name with a masked value, so {@code Authorization},
 * {@code Cookie}, {@code X-Api-Key}, {@code npm-otp}, {@code X-NuGet-ApiKey} and {@code
 * Proxy-Authorization} never reach a log. The request body is never read, and neither is a form
 * parameter value (that would consume the body), so an over-quota upload is not buffered into the
 * log.
 *
 * <p>The trace id is supplied by the caller, so the log entry and the {@code errorCode} returned to
 * the client can carry the same value.
 *
 * @param traceId the id shared by the log entry and the response
 * @param method the HTTP method
 * @param path the request URI
 * @param parameterNames the names of the query parameters, in order of appearance
 * @param headers the header names (as sent) with their value, or {@link #MASK} when not allowed
 */
public record RequestReport(
    String traceId,
    String method,
    String path,
    List<String> parameterNames,
    Map<String, String> headers) {

  /** Replaces the value of a header that is not on the allow-list. */
  public static final String MASK = "*************";

  private static final Set<String> ALLOWED_HEADERS =
      Set.of(
          "accept",
          "accept-encoding",
          "accept-language",
          "cache-control",
          "connection",
          "content-encoding",
          "content-length",
          "content-type",
          "expect",
          "host",
          "if-match",
          "if-modified-since",
          "if-none-match",
          "origin",
          "range",
          "referer",
          "transfer-encoding",
          "user-agent",
          "x-forwarded-for",
          "x-forwarded-host",
          "x-forwarded-proto",
          "x-request-id");

  /**
   * Snapshots the request without touching its body.
   *
   * @param request the request that failed
   * @param traceId the id to report with, normally the one returned to the client
   * @return the report
   */
  public static RequestReport of(final HttpServletRequest request, final String traceId) {

    final var headers = new LinkedHashMap<String, String>();
    final Enumeration<String> names = request.getHeaderNames();

    while (names != null && names.hasMoreElements()) {
      final var name = names.nextElement();
      headers.put(
          name,
          ALLOWED_HEADERS.contains(name.toLowerCase(Locale.ROOT)) ? request.getHeader(name) : MASK);
    }

    return new RequestReport(
        traceId,
        request.getMethod(),
        request.getRequestURI(),
        parameterNames(request.getQueryString()),
        headers);
  }

  private static List<String> parameterNames(final String queryString) {

    final var result = new ArrayList<String>();

    if (queryString == null || queryString.isEmpty()) {
      return result;
    }

    for (final var pair : queryString.split("&")) {
      final var name = pair.contains("=") ? pair.substring(0, pair.indexOf('=')) : pair;

      if (!name.isEmpty()) {
        result.add(name);
      }
    }

    return result;
  }

  @Override
  public String toString() {

    final var out = new StringBuilder();

    out.append("\nRequest -------------------------------------------\n")
        .append("Trace Id: ")
        .append(this.traceId)
        .append("\nPath: ")
        .append(this.path)
        .append("\nMethod: ")
        .append(this.method)
        .append('\n');

    if (!this.parameterNames.isEmpty()) {
      out.append("Query Parameters: ").append(this.parameterNames).append('\n');
    }

    out.append("Headers -------------------------------------------\n");
    this.headers.forEach((name, value) -> out.append(name).append(": ").append(value).append('\n'));

    return out.toString();
  }
}
