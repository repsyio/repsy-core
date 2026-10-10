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
import java.sql.SQLException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Renders an exception together with its {@link RequestReport} as one loggable string.
 *
 * <p>The report never contains a value taken from the request: not the value that failed to
 * convert, not the value a validator rejected, and not the message of an exception that embeds
 * either. Exceptions known to embed user input in their message (see {@link #embedsInput}) are
 * printed as class names and stack frames only, for the whole cause chain, so a password or token
 * in a parameter or JSON body cannot reach a log. Other exceptions keep their message, which is
 * written by the application, not copied from the request.
 */
@UtilityClass
public class ExceptionReport {

  /**
   * Exception types whose message can carry user input: failed conversions and type mismatches
   * ("for value 'x'"), unreadable bodies (a JSON fragment), bind and validation errors, number and
   * date parsing ("For input string"), and database errors ("Key (email)=(x) already exists").
   */
  private static final List<Class<? extends Throwable>> INPUT_EMBEDDING =
      List.of(
          ConversionException.class,
          TypeMismatchException.class,
          HttpMessageNotReadableException.class,
          BindException.class,
          NumberFormatException.class,
          DateTimeParseException.class,
          SQLException.class,
          DataAccessException.class);

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
        + stackTrace(ex);
  }

  public static String of(
      final HttpRequestMethodNotSupportedException ex, final RequestReport request) {

    // The message only echoes the method, which RequestReport already carries.
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

    // Field, code and default message only: FieldError#toString() would add the rejected value.
    for (final var error : ex.getBindingResult().getAllErrors()) {
      out.append("Object: ").append(error.getObjectName());

      if (error instanceof FieldError fieldError) {
        out.append(", Field: ").append(fieldError.getField());
      }

      out.append(", Code: ")
          .append(error.getCode())
          .append(", Message: ")
          .append(error.getDefaultMessage())
          .append("\n");
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

    if (!embedsInput(ex)) {
      final var writer = new StringWriter();
      ex.printStackTrace(new PrintWriter(writer));

      return writer.toString();
    }

    final var out = new StringBuilder();
    appendWithoutMessages(out, ex, "", Collections.newSetFromMap(new IdentityHashMap<>()));

    return out.toString();
  }

  /**
   * Whether the throwable, or anything in its cause or suppressed chain, can carry user input in
   * its message: failed conversions and type mismatches ("for value 'x'"), unreadable bodies (a
   * JSON fragment), bind and validation errors, number and date parsing ("For input string"), and
   * database errors ("Key (email)=(x) already exists").
   */
  private static boolean embedsInput(final Throwable ex) {

    final Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());

    return embedsInput(ex, seen);
  }

  private static boolean embedsInput(final Throwable ex, final Set<Throwable> seen) {

    if (ex == null || !seen.add(ex)) {
      return false;
    }

    if (INPUT_EMBEDDING.stream().anyMatch(type -> type.isInstance(ex))) {
      return true;
    }

    final var related = new ArrayList<Throwable>(Arrays.asList(ex.getSuppressed()));
    related.add(ex.getCause());

    return related.stream().anyMatch(other -> embedsInput(other, seen));
  }

  private static void appendWithoutMessages(
      final StringBuilder out, final Throwable ex, final String prefix, final Set<Throwable> seen) {

    if (!seen.add(ex)) {
      out.append(prefix).append("[CIRCULAR REFERENCE: ").append(ex.getClass().getName());
      out.append("]\n");
      return;
    }

    out.append(prefix).append(ex.getClass().getName());

    out.append(declaredNames(ex));
    out.append('\n');

    for (final var frame : ex.getStackTrace()) {
      out.append(prefix).append("\tat ").append(frame).append('\n');
    }

    for (final var suppressed : ex.getSuppressed()) {
      appendWithoutMessages(out, suppressed, prefix + "\t", seen);
    }

    final var cause = ex.getCause();

    if (cause != null) {
      out.append(prefix).append("Caused by: ");
      appendWithoutMessages(out, cause, prefix, seen);
    }
  }

  /** Property, parameter and type names are declared by the server, not sent by the client. */
  private static String declaredNames(final Throwable ex) {

    if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
      return " [parameter="
          + mismatch.getName()
          + ", requiredType="
          + mismatch.getRequiredType()
          + "]";
    }

    if (ex instanceof TypeMismatchException mismatch) {
      return " [property="
          + mismatch.getPropertyName()
          + ", requiredType="
          + mismatch.getRequiredType()
          + "]";
    }

    return "";
  }
}
