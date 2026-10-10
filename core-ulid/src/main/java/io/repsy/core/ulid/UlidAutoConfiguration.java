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
package io.repsy.core.ulid;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registers the {@link UlidConverter} in every Spring Boot application that has core-ulid on its
 * classpath, so applications need not component-scan {@code io.repsy.core.ulid}.
 */
@AutoConfiguration
public class UlidAutoConfiguration {

  /**
   * Creates the converter that Spring's conversion service uses to render a ULID as a string.
   *
   * @return the converter
   */
  @Bean
  @ConditionalOnMissingBean
  UlidConverter ulidConverter() {
    return new UlidConverter();
  }
}
