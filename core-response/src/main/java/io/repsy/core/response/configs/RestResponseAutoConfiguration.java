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

import io.repsy.core.response.services.RestResponseFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;

/**
 * Registers the {@link RestResponseFactory} in every Spring Boot application that has core-response
 * on its classpath, so applications need not component-scan {@code io.repsy.core.response}.
 */
@AutoConfiguration(
    afterName = "org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration")
public class RestResponseAutoConfiguration {

  /**
   * Creates the factory on the application's {@link MessageSource}. An application bean of the same
   * type wins.
   *
   * @param messageSource resolves the response texts from message ids
   * @return the factory
   */
  @Bean
  @ConditionalOnMissingBean
  RestResponseFactory restResponseFactory(final MessageSource messageSource) {
    return new RestResponseFactory(messageSource);
  }
}
