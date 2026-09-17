/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package io.repsy.core.error_handling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.repsy.core.error_handling.exceptions.AccessNotAllowedException;
import io.repsy.core.error_handling.exceptions.BadRequestException;
import io.repsy.core.error_handling.exceptions.CryptoException;
import io.repsy.core.error_handling.exceptions.DataExportRequestException;
import io.repsy.core.error_handling.exceptions.ErrorOccurredException;
import io.repsy.core.error_handling.exceptions.EventResponseException;
import io.repsy.core.error_handling.exceptions.EventTimeoutException;
import io.repsy.core.error_handling.exceptions.ItemAlreadyExistException;
import io.repsy.core.error_handling.exceptions.ItemNotFoundException;
import io.repsy.core.error_handling.exceptions.JsonParseException;
import io.repsy.core.error_handling.exceptions.ManifestListResolutionException;
import io.repsy.core.error_handling.exceptions.ManifestParseException;
import io.repsy.core.error_handling.exceptions.ManifestSerializationException;
import io.repsy.core.error_handling.exceptions.MfaException;
import io.repsy.core.error_handling.exceptions.RedirectToPathException;
import io.repsy.core.error_handling.exceptions.RetryableException;
import io.repsy.core.error_handling.exceptions.SignatureNotVerifiedException;
import io.repsy.core.error_handling.exceptions.SslContextInitializationException;
import io.repsy.core.error_handling.exceptions.SubscriptionLimitReachedException;
import io.repsy.core.error_handling.exceptions.UnAuthorizedException;
import io.repsy.core.error_handling.utils.ErrorUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

class ErrorHandlingTest {
  @Test
  void exceptionTypesPreserveMessagesAndCauses() {
    final var cause = new IllegalStateException("cause");
    assertEquals("id", new AccessNotAllowedException("id").getMessage());
    assertEquals("id", new BadRequestException("id").getMessage());
    assertEquals("id", new DataExportRequestException("id").getMessage());
    assertEquals("id", new ItemAlreadyExistException("id").getMessage());
    assertEquals("id", new ItemNotFoundException("id").getMessage());
    assertEquals("id", new MfaException("id").getMessage());
    assertEquals("id", new SignatureNotVerifiedException("id").getMessage());
    assertEquals("id", new SubscriptionLimitReachedException("id").getMessage());
    assertEquals("message", new EventTimeoutException("message").getMessage());
    assertEquals("message", new RetryableException("message").getMessage());
    assertEquals("/target", new RedirectToPathException("/target").getPath());
    assertEquals(
        Map.of("X-Test", "yes"),
        new UnAuthorizedException("id", Map.of("X-Test", "yes")).getHeaders());
    assertEquals("id", new UnAuthorizedException("id").getMessage());
    assertEquals(cause, new CryptoException("message", cause).getCause());
    assertEquals(cause, new EventResponseException("message", cause).getCause());
    assertEquals(cause, new JsonParseException("message", cause).getCause());
    assertEquals(cause, new ManifestParseException("message", cause).getCause());
    assertEquals(cause, new ManifestSerializationException("message", cause).getCause());
    assertEquals(cause, new SslContextInitializationException("message", cause).getCause());
    assertEquals(cause, new ErrorOccurredException(cause).getCause());
    assertEquals("custom", new ErrorOccurredException("custom", cause).getMessage());
    assertEquals(cause, new ErrorOccurredException("custom", cause).getCause());
    assertEquals("message", new ManifestListResolutionException("message").getMessage());
    assertEquals(cause, new ManifestListResolutionException("message", cause).getCause());
  }

  @Test
  void errorUtilsFormatsRequestsAndSpecializedExceptions() throws Exception {
    final var request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/test");
    when(request.getMethod()).thenReturn("POST");
    when(request.getQueryString()).thenReturn("a=1");
    when(request.getParameterNames())
        .thenReturn(Collections.enumeration(Map.of("a", "1").keySet()));
    when(request.getParameter("a")).thenReturn("1");
    final var headerNames = new Vector<String>();
    headerNames.add("Authorization");
    headerNames.add("X-Trace");
    when(request.getHeaderNames()).thenReturn(headerNames.elements());
    when(request.getHeader("Authorization")).thenReturn("secret");
    when(request.getHeader("X-Trace")).thenReturn("trace");
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader("body")));

    final var generic = ErrorUtils.exceptionToString(new IllegalArgumentException("bad"), request);
    assertTrue(generic.contains("/api/test"));
    assertTrue(generic.contains("Authorization: *************"));
    assertTrue(generic.contains("Content"));

    final var conversion =
        new ConversionFailedException(
            TypeDescriptor.valueOf(String.class),
            TypeDescriptor.valueOf(Integer.class),
            "x",
            new IllegalArgumentException());
    assertTrue(ErrorUtils.exceptionToString(conversion, request).contains("Source Type"));
    final var method = new HttpRequestMethodNotSupportedException("POST", java.util.List.of("GET"));
    assertTrue(ErrorUtils.exceptionToString(method, request).contains("Supported HTTP Methods"));
    final var missing = new MissingServletRequestParameterException("limit", "int");
    assertTrue(
        ErrorUtils.exceptionToString(missing, request).contains("Missing Parameter Name: limit"));
    assertNotNull(ErrorUtils.exceptionToString(new IllegalArgumentException(), request));
  }

  @Test
  void specializedExceptionValuesAreAvailable() {
    final var cause = new IllegalArgumentException();
    final var exception = new ErrorOccurredException(cause);
    assertSame(cause, exception.getCause());
  }
}
