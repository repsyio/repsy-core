/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package io.repsy.core.uuidv7;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.UUID;
import org.hibernate.generator.EventType;
import org.junit.jupiter.api.Test;

class UuidV7Test {
  @Test
  void generatorProducesUuidV7AndInsertEvents() {
    final var generator = new UuidV7Generator();
    final var generated = generator.generate(null, new Object(), null, EventType.INSERT);
    assertTrue(generated.version() == 7);
    assertEquals(EnumSet.of(EventType.INSERT), generator.getEventTypes());
  }

  @Test
  void annotationIsRuntimeVisible() throws Exception {
    assertTrue(UuidV7.class.isAnnotation());
    assertTrue(UuidV7.class.isAnnotationPresent(java.lang.annotation.Retention.class));
    assertEquals(
        UUID.class,
        UuidV7Generator.class
            .getMethod(
                "generate",
                org.hibernate.engine.spi.SharedSessionContractImplementor.class,
                Object.class,
                Object.class,
                EventType.class)
            .getReturnType());
  }
}
