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

import java.io.PrintWriter;
import java.io.StringWriter;
import lombok.experimental.UtilityClass;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

/** Renders an exception together with its {@link RequestReport} as one loggable string. */
@UtilityClass
public class ExceptionReport {

  private static final String STACK_TRACE_SEPARATOR =
      "Stack Trace ---------------------------------------\n";

  public static String of(final Throwable ex, final RequestReport request) {

    return header(request) + stackTrace(ex);
  }

  public static String of(final ConversionFailedException ex, final RequestReport request) {

    return header(request)
        + "Source Type: "
        + ex.getSourceType()
        + "\n"
        + "Target Type: "
        + ex.getTargetType()
        + "\n"
        + "Value: "
        + ex.getValue()
        + "\n"
        + stackTrace(ex);
  }

  public static String of(
      final HttpRequestMethodNotSupportedException ex, final RequestReport request) {

    return header(request)
        + "Method: "
        + ex.getMethod()
        + "\n"
        + "Supported HTTP Methods: "
        + ex.getSupportedHttpMethods()
        + "\n"
        + stackTrace(ex);
  }

  public static String of(final MethodArgumentNotValidException ex, final RequestReport request) {

    final var out = new StringBuilder(header(request));

    for (final var error : ex.getBindingResult().getFieldErrors()) {
      out.append(error).append("\n");
    }

    return out.append(stackTrace(ex)).toString();
  }

  public static String of(
      final MissingServletRequestParameterException ex, final RequestReport request) {

    return header(request)
        + "Missing Parameter Name: "
        + ex.getParameterName()
        + "\n"
        + "Missing Parameter's Type: "
        + ex.getParameterType()
        + "\n"
        + stackTrace(ex);
  }

  private static String header(final RequestReport request) {

    return request + STACK_TRACE_SEPARATOR + "Error Code: " + request.traceId() + "\n";
  }

  private static String stackTrace(final Throwable ex) {

    final var writer = new StringWriter();
    ex.printStackTrace(new PrintWriter(writer));

    return writer.toString();
  }
}
