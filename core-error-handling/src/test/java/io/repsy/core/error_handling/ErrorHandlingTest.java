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
import static org.junit.jupiter.api.Assertions.assertSame;

import io.repsy.core.error_handling.exceptions.AccessNotAllowedException;
import io.repsy.core.error_handling.exceptions.BadRequestException;
import io.repsy.core.error_handling.exceptions.BaseException;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ErrorHandlingTest {
  @Test
  void everyBaseExceptionTypeIsExercisedHere() {
    final var known =
        List.of(
            "AccessNotAllowedException",
            "BadRequestException",
            "CryptoException",
            "DataExportRequestException",
            "ErrorOccurredException",
            "EventResponseException",
            "ItemAlreadyExistException",
            "ItemNotFoundException",
            "JsonParseException",
            "ManifestListResolutionException",
            "ManifestParseException",
            "ManifestSerializationException",
            "MfaException",
            "SignatureNotVerifiedException",
            "SslContextInitializationException",
            "SubscriptionLimitReachedException",
            "UnAuthorizedException");
    final var actual = new java.util.TreeSet<String>();
    final var queue = new java.util.ArrayDeque<Class<?>>(List.of(BaseException.class));
    while (!queue.isEmpty()) {
      final var sub = queue.poll().getPermittedSubclasses();
      if (sub == null) {
        continue;
      }
      for (final var c : sub) {
        if (c.getPermittedSubclasses() == null) {
          actual.add(c.getSimpleName());
        }
        queue.add(c);
      }
    }

    assertEquals(known, List.copyOf(actual), "New exception type: add tests for it in this class");
  }

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
    assertEquals("errorOccurred", new ErrorOccurredException(cause).getMessage());
    assertEquals("message", new ManifestListResolutionException("message").getMessage());
    assertEquals(cause, new ManifestListResolutionException("message", cause).getCause());
  }

  @Test
  void specializedExceptionValuesAreAvailable() {
    final var cause = new IllegalArgumentException();
    final var exception = new ErrorOccurredException(cause);
    assertSame(cause, exception.getCause());
  }

  @Test
  void errorOccurredExceptionAcceptsOnlyACauseSoNoFreeTextReachesTheClient() {
    final var parameterTypes =
        Arrays.stream(ErrorOccurredException.class.getConstructors())
            .map(constructor -> List.of(constructor.getParameterTypes()))
            .toList();

    assertEquals(List.of(List.<Class<?>>of(Exception.class)), parameterTypes);
  }
}
