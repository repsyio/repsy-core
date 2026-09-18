/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package io.repsy.core.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventTest {
  private static final UUID UUID_VALUE = UUID.fromString("11111111-1111-7111-8111-111111111111");
  private static final Instant INSTANT_VALUE = Instant.parse("2026-01-01T00:00:00Z");

  @Test
  void allEventRecordsExposeTheirPayload() throws Exception {
    final var eventTypes =
        new String[] {
          "ArtifactPushedEvent",
          "ArtifactVersionDeletedEvent",
          "ContactRequestReceivedEvent",
          "DeploymentNotificationRequestedEvent",
          "DeploymentScanResultNotificationRequestedEvent",
          "EmailOtpRequestedEvent",
          "EmailUpdatedEvent",
          "EmailVerificationRequestedEvent",
          "GdprRequestedEvent",
          "MailjetUnsubRequestedEvent",
          "PasswordRecoveryRequestedEvent",
          "PasswordUpdatedEvent",
          "RegistrationCompletedEvent",
          "ScanResultNotificationRequestedEvent",
          "SeverityCounts",
          "TenantDeleteRequestedEvent",
          "TenantInactivityWarningEvent",
          "UserCreatedEvent",
          "UserLoginEvent",
          "UsernameUpdatedEvent"
        };

    for (final var eventType : eventTypes) {
      final var eventClass = Class.forName("io.repsy.core.events." + eventType);
      final var components = eventClass.getRecordComponents();
      final var values = new Object[components.length];
      for (var index = 0; index < components.length; index++) {
        values[index] = valueFor(components[index]);
      }
      final var event = eventClass.getDeclaredConstructors()[0].newInstance(values);
      final var sameValuesEvent = eventClass.getDeclaredConstructors()[0].newInstance(values);
      for (var index = 0; index < components.length; index++) {
        assertEquals(values[index], components[index].getAccessor().invoke(event));
      }
      assertNotNull(event.toString());
      assertEquals(event, sameValuesEvent);
      assertEquals(event.hashCode(), sameValuesEvent.hashCode());
    }
  }

  @Test
  void buildersAndResolvableTypesWork() {
    final var variables = java.util.Map.of("category", "support");
    final var contact =
        ContactRequestReceivedEvent.builder()
            .templateId(42L)
            .name("Ada")
            .email("ada@example.test")
            .message("Help")
            .variables(variables)
            .build();
    assertEquals(42L, contact.templateId());
    assertEquals(variables, contact.variables());

    final var registration =
        RegistrationCompletedEvent.builder()
            .username("ada")
            .hash(null)
            .salt("salt")
            .tenantUuid(UUID_VALUE)
            .fullName("Ada Lovelace")
            .email("ada@example.test")
            .verificationCode("code")
            .build();
    assertEquals("ada", registration.username());
    assertEquals(UUID_VALUE, registration.tenantUuid());

    final var userCreated = new UserCreatedEvent<>(UUID_VALUE, "ada");
    assertEquals(UUID.class, userCreated.getResolvableType().getGeneric(0).resolve());
  }

  @Test
  void emptyEventIsConstructible() {
    assertNotNull(new ClearStorageTrashRequestedEvent());
    assertTrue(ClearStorageTrashRequestedEvent.class.getDeclaredConstructors().length > 0);
  }

  private static Object valueFor(final RecordComponent component) {
    final var type = component.getType();
    if (type == String.class) {
      return component.getName();
    }
    if (type == UUID.class) {
      return UUID_VALUE;
    }
    if (type == Instant.class) {
      return INSTANT_VALUE;
    }
    if (type == SeverityCounts.class) {
      return new SeverityCounts("1", "2", "3", "4", "5");
    }
    if (type == Map.class) {
      return Map.of("key", "value");
    }
    if (type == long.class) {
      return 7L;
    }
    if (type == int.class) {
      return 2;
    }
    if (type == boolean.class) {
      return true;
    }
    return "value";
  }
}
