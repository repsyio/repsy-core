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
package io.repsy.core.error_handling;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.repsy.core.error_handling.exceptions.AccessNotAllowedException;
import io.repsy.core.error_handling.exceptions.BadRequestException;
import io.repsy.core.error_handling.exceptions.DataExportRequestException;
import io.repsy.core.error_handling.exceptions.ItemAlreadyExistException;
import io.repsy.core.error_handling.exceptions.ItemNotFoundException;
import io.repsy.core.error_handling.exceptions.ManifestListResolutionException;
import io.repsy.core.error_handling.exceptions.MfaException;
import io.repsy.core.error_handling.exceptions.SignatureNotVerifiedException;
import io.repsy.core.error_handling.exceptions.SubscriptionLimitReachedException;
import io.repsy.core.error_handling.exceptions.UnAuthorizedException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** The exceptions that carry a msgId accept a bare identifier only (RPS-1127). */
class MsgIdValidationTest {

  private record Factory(String name, Function<String, RuntimeException> create) {
    @Override
    public String toString() {
      return name;
    }
  }

  static Stream<Factory> msgIdExceptions() {
    return Stream.of(
        new Factory("AccessNotAllowedException", AccessNotAllowedException::new),
        new Factory("BadRequestException", BadRequestException::new),
        new Factory("DataExportRequestException", DataExportRequestException::new),
        new Factory("ItemAlreadyExistException", ItemAlreadyExistException::new),
        new Factory("ItemNotFoundException", ItemNotFoundException::new),
        new Factory("MfaException", MfaException::new),
        new Factory("SignatureNotVerifiedException", SignatureNotVerifiedException::new),
        new Factory("SubscriptionLimitReachedException", SubscriptionLimitReachedException::new),
        new Factory("UnAuthorizedException", UnAuthorizedException::new),
        new Factory(
            "UnAuthorizedException with headers", id -> new UnAuthorizedException(id, Map.of())));
  }

  @ParameterizedTest
  @MethodSource("msgIdExceptions")
  void bareIdentifiersAreKept(final Factory factory) {
    for (final var id : new String[] {"itemNotFound", "ERR_UNAUTHORIZED", "repo2", "_x", "A"}) {
      assertEquals(id, factory.create().apply(id).getMessage());
    }
  }

  @ParameterizedTest
  @MethodSource("msgIdExceptions")
  void noIdIsAccepted(final Factory factory) {
    assertNull(assertDoesNotThrow(() -> factory.create().apply(null)).getMessage());
  }

  @ParameterizedTest
  @MethodSource("msgIdExceptions")
  void freeTextIsRejected(final Factory factory) {
    for (final var text :
        new String[] {
          "crate `a@1` already exists in this registry",
          "Invalid manifest path format: /v2/../etc",
          "no public key found with Id 42",
          "key.with.dots",
          "dash-ed",
          "trailing ",
          " leading",
          "line\nbreak",
          "trailing\n",
          "ünicode",
          "",
          "%s"
        }) {
      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> factory.create().apply(text));

      assertEquals(
          "A msgId must be a bare identifier ([A-Za-z0-9_]+) with an entry in messages.properties,"
              + " not free text; log the details instead",
          thrown.getMessage());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"a sentence.", "user@example.com", "/api/v1/repos"})
  void theRejectionDoesNotEchoTheText(final String text) {
    final var thrown =
        assertThrows(IllegalArgumentException.class, () -> new BadRequestException(text));

    assertEquals(false, thrown.getMessage().contains(text));
  }

  @Test
  void exceptionsWithoutAMsgIdKeepFreeTextMessages() {
    assertEquals(
        "free text, not an id",
        new ManifestListResolutionException("free text, not an id").getMessage());
  }
}
