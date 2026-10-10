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
package io.repsy.core.response.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.repsy.core.response.dtos.RestResponse;
import io.repsy.core.response.services.RestResponseFactory;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;

class RestResponseAutoConfigurationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(RestResponseAutoConfiguration.class));

  @AfterEach
  void resetLocale() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void isListedInTheAutoConfigurationImports() {
    final var candidates =
        ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates();

    assertTrue(candidates.contains("io.repsy.core.response.configs.RestResponseAutoConfiguration"));
  }

  @Test
  void registersTheFactoryWithoutAComponentScan() {
    this.runner.run(context -> assertNotNull(context.getBean(RestResponseFactory.class)));
  }

  @Test
  void backsOffForAnApplicationFactory() {
    final var own = new RestResponseFactory(new StaticMessageSource());

    this.runner
        .withBean(RestResponseFactory.class, () -> own)
        .run(context -> assertSame(own, context.getBean(RestResponseFactory.class)));
  }

  @Test
  void resolvesTheTextInTheLocaleOfTheCurrentRequest() {
    final var messages = new StaticMessageSource();
    messages.addMessage("greeting", Locale.ENGLISH, "hello");
    messages.addMessage("greeting", Locale.GERMAN, "hallo");
    this.runner
        .withBean("messageSource", MessageSource.class, () -> messages)
        .run(
            context -> {
              final var factory = context.getBean(RestResponseFactory.class);

              LocaleContextHolder.setLocale(Locale.GERMAN);
              final RestResponse<Object> german = factory.success("greeting");
              LocaleContextHolder.setLocale(Locale.ENGLISH);
              final RestResponse<Object> english = factory.success("greeting");

              assertEquals("hallo", german.getText());
              assertEquals("hello", english.getText());
            });
  }
}
