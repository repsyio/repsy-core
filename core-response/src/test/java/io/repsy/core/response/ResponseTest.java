/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package io.repsy.core.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.repsy.core.response.dtos.ResponseType;
import io.repsy.core.response.dtos.RestResponse;
import io.repsy.core.response.services.RestResponseFactory;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;

class ResponseTest {
  private final MessageSource messages =
      new MessageSource() {
        @Override
        public String getMessage(
            final String code,
            final Object[] args,
            final String defaultMessage,
            final Locale locale) {
          return "translated:" + code;
        }

        @Override
        public String getMessage(final String code, final Object[] args, final Locale locale) {
          return "translated:" + code;
        }

        @Override
        public String getMessage(final MessageSourceResolvable resolvable, final Locale locale) {
          return "translated:resolvable";
        }
      };

  @Test
  void restResponseStoresAndExposesItsValues() {
    final var response = new RestResponse<String>("id", ResponseType.SUCCESS);
    response.setData("data");
    response.setText("text");
    response.setErrorCode("error");

    assertEquals("id", response.getMsgId());
    assertEquals(ResponseType.SUCCESS, response.getType());
    assertEquals("data", response.getData());
    assertEquals("text", response.getText());
    assertEquals("error", response.getErrorCode());
  }

  @Test
  void factoryCreatesAllResponseKindsAndOverloads() {
    final var factory = new RestResponseFactory(messages);
    final var error = factory.<String>error("bad", "payload");
    final var errorWithoutData = factory.<String>error("bad");
    final var success = factory.<String>success("ok", "payload");
    final var successWithoutData = factory.<String>success("ok");
    final var warning = factory.<String>warning("warn", null);
    final var warningWithoutData = factory.<String>warning("warn");

    assertEquals(ResponseType.ERROR, error.getType());
    assertNotNull(error.getErrorCode());
    assertEquals("payload", error.getData());
    assertEquals("translated:bad", error.getText());
    assertEquals(ResponseType.ERROR, errorWithoutData.getType());
    assertNull(errorWithoutData.getData());
    assertEquals(ResponseType.SUCCESS, success.getType());
    assertEquals("payload", success.getData());
    assertNull(successWithoutData.getData());
    assertEquals(ResponseType.WARNING, warning.getType());
    assertNull(warning.getData());
    assertNull(warningWithoutData.getData());
    assertEquals(Locale.getDefault(), Locale.getDefault());
  }
}
