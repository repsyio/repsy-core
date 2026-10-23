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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.repsy.core.error_handling.utils.ErrorUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

class RequestReportTest {

  private static HttpServletRequest request(final Map<String, String> headers, final String query) {
    final var request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/test");
    when(request.getMethod()).thenReturn("POST");
    when(request.getQueryString()).thenReturn(query);
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(headers.keySet()));
    headers.forEach((k, v) -> when(request.getHeader(k)).thenReturn(v));
    return request;
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Authorization",
        "authorization",
        "Cookie",
        "X-Api-Key",
        "npm-otp",
        "X-NuGet-ApiKey",
        "Proxy-Authorization"
      })
  void secretHeadersAreMaskedRegardlessOfCase(final String name) throws Exception {
    final var report = RequestReport.of(request(Map.of(name, "s3cret"), null), "t1").toString();

    assertTrue(report.contains(name + ": " + RequestReport.MASK));
    assertFalse(report.contains("s3cret"));
  }

  @Test
  void allowedHeadersAreKeptAndUnknownOnesMasked() throws Exception {
    final var report =
        RequestReport.of(
                request(Map.of("User-Agent", "curl/8", "X-Custom-Token", "abc"), null), "t1")
            .toString();

    assertTrue(report.contains("User-Agent: curl/8"));
    assertTrue(report.contains("X-Custom-Token: " + RequestReport.MASK));
    assertFalse(report.contains("abc"));
  }

  @Test
  void bodyAndParameterValuesAreNeverRead() throws Exception {
    final var request = request(Map.of(), "token=abc&flag&a=1");

    final var report = RequestReport.of(request, "t1");

    verify(request, never()).getReader();
    verify(request, never()).getInputStream();
    verify(request, never()).getParameter("token");
    verify(request, never()).getParameterNames();
    assertEquals(List.of("token", "flag", "a"), report.parameterNames());
    assertFalse(report.toString().contains("abc"));
  }

  @Test
  void traceIdIsEchoedInTheReport() throws Exception {
    final var report = RequestReport.of(request(Map.of(), null), "trace-123");

    assertEquals("trace-123", report.traceId());
    assertTrue(report.toString().contains("Trace Id: trace-123"));
    assertTrue(ExceptionReport.of(new IllegalStateException("x"), report).contains("trace-123"));
    assertTrue(report.toString().contains("/api/test"));
  }

  @Test
  void exceptionReportsDescribeTheSpecialisedExceptions() throws Exception {
    final var report = RequestReport.of(request(Map.of(), "a=1"), "t1");

    final var conversion =
        new ConversionFailedException(
            TypeDescriptor.valueOf(String.class),
            TypeDescriptor.valueOf(Integer.class),
            "x",
            new IllegalArgumentException());
    assertTrue(ExceptionReport.of(conversion, report).contains("Source Type"));
    assertTrue(
        ExceptionReport.of(
                new HttpRequestMethodNotSupportedException("POST", List.of("GET")), report)
            .contains("Supported HTTP Methods"));
    assertTrue(
        ExceptionReport.of(new MissingServletRequestParameterException("limit", "int"), report)
            .contains("Missing Parameter Name: limit"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void deprecatedErrorUtilsDelegatesWithoutReadingTheBody() throws Exception {
    final var request = request(Map.of("Authorization", "s3cret"), "a=1");

    final var text = ErrorUtils.exceptionToString(new IllegalArgumentException("bad"), request);

    assertTrue(text.contains("/api/test"));
    assertTrue(text.contains("Trace Id: "));
    assertFalse(text.contains("s3cret"));
    verify(request, never()).getReader();
  }

  @Test
  void refererUserinfoAndQueryNeverReachTheReport() throws Exception {
    final var report =
        RequestReport.of(
                request(Map.of("Referer", "https://user:pw@host/p?token=abc#frag"), null), "t1")
            .toString();

    assertTrue(report.contains("Referer: https://host\n"));
    assertFalse(report.contains("user"));
    assertFalse(report.contains("pw"));
    assertFalse(report.contains("token"));
    assertFalse(report.contains("abc"));
  }

  @Test
  void malformedUserinfoWithAtSignDoesNotLeak() throws Exception {
    final var report =
        RequestReport.of(request(Map.of("Referer", "https://u:p@ss@host/"), null), "t1").toString();

    assertTrue(report.contains("Referer: https://host\n"));
    assertFalse(report.contains("ss"));
  }

  @Test
  void plainRefererStaysUseful() throws Exception {
    final var report =
        RequestReport.of(
                request(
                    Map.of(
                        "Referer", "HTTPS://Example.com:8443/docs/page",
                        "Origin", "http://localhost:4200",
                        "X-Forwarded-Host", "repo.repsy.io, [::1]:80"),
                    null),
                "t1")
            .toString();

    assertTrue(report.contains("Referer: https://Example.com:8443\n"));
    assertTrue(report.contains("Origin: http://localhost:4200\n"));
    assertTrue(report.contains("X-Forwarded-Host: repo.repsy.io, [::1]:80\n"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "secret-token",
        "https://",
        "https://:8080/",
        "https://u:p/x@host",
        "https://SECRET#x@host/",
        "https://SECRET/..@host",
        "https://host\\@evil",
        "https://host/@scope/pkg",
        "https://ho st/",
        "https://host:80a/",
        "https://host:123456/",
        "https://[::1/",
        "https://[zz]/",
        "https://[]/",
        "1http://host/",
        "ht tp://host/",
        "https://host%2e/",
        "https://ho\r\nst/"
      })
  void unparsableRefererFailsClosed(final String value) throws Exception {
    final var report = RequestReport.of(request(Map.of("Referer", value), null), "t1").toString();

    assertTrue(report.contains("Referer: " + LogSanitizer.REDACTED + "\n"), value);
  }

  @Test
  void oversizedAndNullOriginsAreHandled() throws Exception {
    final var long1 = "https://" + "a".repeat(3000);
    final var report =
        RequestReport.of(request(Map.of("Origin", "null", "Referer", long1), null), "t1")
            .toString();

    assertTrue(report.contains("Origin: null\n"));
    assertTrue(report.contains("Referer: ***\n"));
    assertEquals("***", LogSanitizer.forwardedHost("a".repeat(3000)));
    assertEquals("***", LogSanitizer.forwardedHost("a,b,c,d,e,f,g,h,i"));
    assertEquals("***", LogSanitizer.forwardedHost("evil.com/path?token=1"));
    assertEquals("***", LogSanitizer.origin("https://" + "a".repeat(300) + "/"));
  }

  @Test
  void controlCharactersInAllowedHeadersAreEscaped() throws Exception {
    final var report =
        RequestReport.of(request(Map.of("User-Agent", "curl\r\nFake: line\u007f"), null), "t1")
            .toString();

    assertTrue(report.contains("User-Agent: curl\\u000d\\u000aFake: line\\u007f\n"));
    assertEquals(2048, LogSanitizer.escape("a".repeat(5000)).length());
  }

  @Test
  void nonSimpleParameterNamesAreRedacted() throws Exception {
    final var report =
        RequestReport.of(
            request(
                Map.of(),
                "page&filter[name]=x&a.b-c_d=1&sk_live%5Fabc&"
                    + "x".repeat(65)
                    + "=1&k%0d%0aEvil&bad name"),
            "t1");

    assertEquals(
        List.of("page", "filter[name]", "a.b-c_d", "***", "***", "***", "***"),
        report.parameterNames());
    assertFalse(report.toString().contains("sk_live"));
  }
}
