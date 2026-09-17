/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package io.repsy.core.ulid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.f4b6a3.ulid.Ulid;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.EnumSet;
import org.hibernate.generator.EventType;
import org.junit.jupiter.api.Test;

class UlidTest {
  private final Ulid value = Ulid.from("01ARZ3NDEKTSV4RRFFQ69G5FAV");

  @Test
  void userTypeHandlesValuesAndNulls() throws Exception {
    final var type = new UlidUserType();
    assertEquals(java.sql.Types.VARCHAR, type.getSqlType());
    assertEquals(Ulid.class, type.returnedClass());
    assertTrue(type.equals(value, value));
    assertTrue(type.equals(null, null));
    assertFalse(type.equals(value, null));
    assertFalse(type.equals(null, value));
    assertEquals(value.hashCode(), type.hashCode(value));
    assertEquals(0, type.hashCode(null));
    assertEquals(value, type.deepCopy(value));
    assertNull(type.deepCopy(null));
    assertFalse(type.isMutable());
    assertEquals(value.toString(), type.disassemble(value));
    assertNull(type.disassemble(null));
    assertSame(value, type.assemble(value, null));
    assertEquals(value, type.assemble(value.toString(), null));
    assertNull(type.assemble(null, null));
    assertEquals(value.toString(), type.toSqlLiteral(value));
    assertNull(type.toSqlLiteral(null));
    assertEquals(value.toString(), type.toString(value));
    assertNull(type.toString(null));
    assertEquals(value, type.fromStringValue(value.toString()));
    assertNull(type.fromStringValue(null));

    final var rs = org.mockito.Mockito.mock(ResultSet.class);
    org.mockito.Mockito.when(rs.getString(1)).thenReturn(value.toString());
    assertEquals(value, type.nullSafeGet(rs, 1, null, null));
    org.mockito.Mockito.when(rs.getString(1)).thenReturn(null);
    assertNull(type.nullSafeGet(rs, 1, null, null));
    final var statement = org.mockito.Mockito.mock(PreparedStatement.class);
    type.nullSafeSet(statement, value, 2, null);
    type.nullSafeSet(statement, null, 3, null);
    org.mockito.Mockito.verify(statement).setString(2, value.toString());
    org.mockito.Mockito.verify(statement).setString(3, null);
  }

  @Test
  void generatorsReturnExpectedValuesAndEvents() {
    final var generator = new RandomUlidGenerator();
    assertEquals(EnumSet.of(EventType.INSERT), generator.getEventTypes());
    assertSame(value, generator.generate(null, new Object(), value, EventType.INSERT));
    assertTrue(generator.generate(null, new Object(), null, EventType.INSERT) instanceof Ulid);
  }

  @Test
  void converterReturnsUlidText() {
    assertEquals(value.toString(), new UlidConverter().convert(value));
  }
}
