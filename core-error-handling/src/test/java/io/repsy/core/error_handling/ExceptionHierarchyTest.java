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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.repsy.core.error_handling.exceptions.BaseException;
import io.repsy.core.error_handling.exceptions.MsgIdException;
import io.repsy.core.error_handling.exceptions.TechnicalException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Walks the sealed {@link BaseException} hierarchy by reflection, so a new exception type is
 * checked without being listed here (RPS-2052).
 */
class ExceptionHierarchyTest {

  private static final Map<String, Integer> MSG_ID_STATUS =
      Map.of(
          "BadRequestException", 400,
          "UnAuthorizedException", 401,
          "AccessNotAllowedException", 403,
          "ItemNotFoundException", 404,
          "ItemAlreadyExistException", 409,
          "SubscriptionLimitReachedException", 403,
          "MfaException", 401,
          "SignatureNotVerifiedException", 400,
          "DataExportRequestException", 400);

  private static List<Class<?>> leaves(final Class<?> type) {
    final var result = new ArrayList<Class<?>>();
    final var permitted = type.getPermittedSubclasses();
    if (permitted == null) {
      result.add(type);
    } else {
      for (final var sub : permitted) {
        result.addAll(leaves(sub));
      }
    }
    return result;
  }

  private static BaseException instantiate(final Class<?> type)
      throws ReflectiveOperationException {
    for (final Constructor<?> c : type.getConstructors()) {
      final var args = new Object[c.getParameterCount()];
      final var types = c.getParameterTypes();
      for (var i = 0; i < args.length; i++) {
        if (types[i] == String.class) {
          args[i] = "Some free text, with a secret: 42";
        } else if (types[i] == Exception.class) {
          args[i] = new IllegalStateException("cause");
        } else if (types[i] == Throwable.class) {
          args[i] = new IllegalStateException("cause");
        } else if (types[i] == Map.class) {
          args[i] = null;
        } else {
          throw new AssertionError("Teach this test to build " + types[i] + " for " + type);
        }
      }
      if (types.length > 0
          && types[0] == String.class
          && MsgIdException.class.isAssignableFrom(type)) {
        args[0] = "someMsgId";
      }
      return (BaseException) c.newInstance(args);
    }
    throw new AssertionError("No public constructor on " + type);
  }

  @Test
  void everyConcreteExceptionHasAStatusAndASafePublicCode() throws ReflectiveOperationException {
    final var all = leaves(BaseException.class);
    assertEquals(17, all.size(), "A new exception type: add it to the expectations of this test");
    for (final var type : all) {
      assertTrue(Modifier.isPublic(type.getModifiers()), type + " must be public");
      final var ex = instantiate(type);
      assertTrue(ex.status() >= 400 && ex.status() <= 599, type + " status " + ex.status());
      final var code = ex.publicCode();
      assertNotNull(code, type.getSimpleName());
      assertTrue(code.matches("\\w+"), type.getSimpleName() + " code " + code);
    }
  }

  @Test
  void msgIdExceptionsKeepTheirMsgIdAndDefaultStatus() throws ReflectiveOperationException {
    final var types = leaves(MsgIdException.class);
    assertEquals(
        MSG_ID_STATUS.keySet(),
        types.stream().map(Class::getSimpleName).collect(java.util.stream.Collectors.toSet()));
    for (final var type : types) {
      final var ex = instantiate(type);
      assertEquals(MSG_ID_STATUS.get(type.getSimpleName()), ex.status(), type.getSimpleName());
      assertEquals("someMsgId", ex.publicCode());
    }
  }

  @Test
  void technicalExceptionsNeverExposeTheirMessageAsTheCode() throws ReflectiveOperationException {
    final var types = leaves(TechnicalException.class);
    assertEquals(8, types.size());
    for (final var type : types) {
      final var ex = instantiate(type);
      assertEquals(500, ex.status(), type.getSimpleName());
      if (!"ErrorOccurredException".equals(type.getSimpleName())) {
        // ErrorOccurredException's message is itself the fixed code, never caller text.
        assertNotEquals(ex.getMessage(), ex.publicCode(), type.getSimpleName());
      }
      assertTrue(
          TechnicalException.PUBLIC_CODE.equals(ex.publicCode())
              || "errorOccurred".equals(ex.publicCode()),
          type.getSimpleName());
    }
  }
}
