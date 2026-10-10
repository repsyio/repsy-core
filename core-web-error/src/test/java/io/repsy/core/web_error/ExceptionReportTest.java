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
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class ExceptionReportTest {

  private static final String SECRET = "s3cr3t-token-value";

  private static RequestReport report(final Map<String, String> headers, final String query) {
    final var request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/login");
    when(request.getMethod()).thenReturn("POST");
    when(request.getQueryString()).thenReturn(query);
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(headers.keySet()));
    headers.forEach((k, v) -> when(request.getHeader(k)).thenReturn(v));

    return RequestReport.of(request, "trace-1");
  }

  private static RequestReport report() {
    return report(Map.of(), null);
  }

  @SuppressWarnings("unused")
  void target(final int limit) {}

  private static MethodParameter parameter() throws Exception {
    final Method method = ExceptionReportTest.class.getDeclaredMethod("target", int.class);

    return new MethodParameter(method, 0);
  }

  @Test
  void conversionFailureNeverLogsTheConvertedValue() {
    final var ex =
        new ConversionFailedException(
            TypeDescriptor.valueOf(String.class),
            TypeDescriptor.valueOf(Integer.class),
            SECRET,
            new NumberFormatException("For input string: \"" + SECRET + "\""));

    final var out = ExceptionReport.of(ex, report());

    assertThat(out)
        .doesNotContain(SECRET)
        .contains("Source Type: ")
        .contains("Target Type: ")
        .contains("trace-1")
        .contains(ConversionFailedException.class.getName())
        .contains(NumberFormatException.class.getName());
  }

  @Test
  void validationFailureNeverLogsTheRejectedValue() throws Exception {
    final var result = new BeanPropertyBindingResult(new Object(), "loginForm");
    result.addError(
        new FieldError(
            "loginForm",
            "password",
            SECRET,
            false,
            new String[] {"Size.loginForm.password", "Size.password"},
            null,
            "size must be between 8 and 64"));
    final var ex = new MethodArgumentNotValidException(parameter(), result);

    final var out = ExceptionReport.of(ex, report());

    assertThat(out)
        .doesNotContain(SECRET)
        .contains("Field: password")
        .contains("Code: Size.password")
        .contains("size must be between 8 and 64")
        .contains("trace-1");
  }

  @Test
  void genericOverloadSanitisesATypeMismatchAndItsCauseChain() throws Exception {
    final var ex =
        new MethodArgumentTypeMismatchException(
            SECRET,
            Integer.class,
            "limit",
            parameter(),
            new IllegalArgumentException("Invalid value '" + SECRET + "'"));

    final var out = ExceptionReport.of((Throwable) ex, report());

    assertThat(out)
        .doesNotContain(SECRET)
        .contains("parameter=limit")
        .contains(MethodArgumentTypeMismatchException.class.getName())
        .contains("Caused by: " + IllegalArgumentException.class.getName());
  }

  @Test
  void typeMismatchExposesOnlyPropertyAndRequiredType() {
    final var ex = new TypeMismatchException(SECRET, Integer.class);
    ex.initPropertyName("age");

    final var out = ExceptionReport.of((Throwable) ex, report());

    assertThat(out).doesNotContain(SECRET).contains("property=age").contains("Integer");
  }

  @Test
  void unreadableBodyNeverLogsTheJsonFragment() {
    final var input = mock(HttpInputMessage.class);
    final var ex =
        new HttpMessageNotReadableException(
            "JSON parse error: Unrecognized token '" + SECRET + "'",
            new IllegalStateException("at [Source: {\"password\":\"" + SECRET + "\"}]"),
            input);

    final var out = ExceptionReport.of((Throwable) ex, report());

    assertThat(out)
        .doesNotContain(SECRET)
        .contains(HttpMessageNotReadableException.class.getName());
  }

  @Test
  void aSensitiveCauseSanitisesAWrappingException() {
    final var ex =
        new IllegalStateException(
            "wrapper " + SECRET, new NumberFormatException("For input string: \"" + SECRET + "\""));

    final var out = ExceptionReport.of(ex, report());

    assertThat(out).doesNotContain(SECRET).contains(IllegalStateException.class.getName());
  }

  @Test
  void databaseErrorsAreSanitised() {
    final var ex =
        new DataIntegrityViolationException(
            "duplicate", new SQLException("Key (email)=(" + SECRET + ") already exists."));
    ex.addSuppressed(new IllegalStateException("also " + SECRET));

    final var out = ExceptionReport.of(ex, report());

    assertThat(out).doesNotContain(SECRET).contains(SQLException.class.getName());
  }

  @Test
  void circularCauseChainTerminates() {
    final var first = new NumberFormatException(SECRET);
    final var second = new IllegalStateException(SECRET, first);
    first.initCause(second);

    final var out = ExceptionReport.of(second, report());

    assertThat(out).doesNotContain(SECRET).contains("CIRCULAR REFERENCE");
  }

  @Test
  void ordinaryExceptionsKeepTheirMessageAndTraceId() {
    final var out = ExceptionReport.of(new IllegalStateException("quota exceeded"), report());

    assertThat(out).contains("quota exceeded").contains("Error Code: trace-1");
  }

  @Test
  void refererQueryStringAndFragmentAreStripped() {
    final var out =
        report(
                Map.of(
                    "Referer",
                    "https://repsy.io/reset?token=" + SECRET + "#frag",
                    "Origin",
                    "https://repsy.io"),
                "token=" + SECRET)
            .toString();

    assertThat(out)
        .doesNotContain(SECRET)
        .contains("Referer: https://repsy.io/reset\n")
        .contains("Origin: https://repsy.io")
        .contains("Query Parameters: " + List.of("token"));
  }
}
