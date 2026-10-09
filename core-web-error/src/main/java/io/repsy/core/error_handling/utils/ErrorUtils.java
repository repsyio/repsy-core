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
package io.repsy.core.error_handling.utils;

import io.repsy.core.web_error.ExceptionReport;
import io.repsy.core.web_error.RequestReport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

/**
 * Kept so that callers compile after the move to {@code core-web-error}. Each call invents its own
 * trace id, which cannot be correlated with the response, and the request body is no longer logged.
 *
 * @deprecated use {@link ExceptionReport} with {@link RequestReport#of} and the trace id of the
 *     response
 */
@Deprecated
@UtilityClass
public class ErrorUtils {

  public static String exceptionToString(final Throwable ex, final HttpServletRequest request) {

    return ExceptionReport.of(ex, report(request));
  }

  public static String exceptionToString(
      final ConversionFailedException ex, final HttpServletRequest request) {

    return ExceptionReport.of(ex, report(request));
  }

  public static String exceptionToString(
      final HttpRequestMethodNotSupportedException ex, final HttpServletRequest request) {

    return ExceptionReport.of(ex, report(request));
  }

  public static String exceptionToString(
      final MethodArgumentNotValidException ex, final HttpServletRequest request) {

    return ExceptionReport.of(ex, report(request));
  }

  public static String exceptionToString(
      final MissingServletRequestParameterException ex, final HttpServletRequest request) {

    return ExceptionReport.of(ex, report(request));
  }

  private static RequestReport report(final HttpServletRequest request) {

    return RequestReport.of(request, UUID.randomUUID().toString());
  }
}
