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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.f4b6a3.ulid.Ulid;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class UlidAutoConfigurationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(UlidAutoConfiguration.class));

  @Test
  void isListedInTheAutoConfigurationImports() {
    final var candidates =
        ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates();

    assertTrue(candidates.contains("io.repsy.core.ulid.UlidAutoConfiguration"));
  }

  @Test
  void registersTheConverterWithoutAComponentScan() {
    this.runner.run(
        context -> {
          final var ulid = Ulid.from("01ARZ3NDEKTSV4RRFFQ69G5FAV");

          assertEquals(
              "01ARZ3NDEKTSV4RRFFQ69G5FAV", context.getBean(UlidConverter.class).convert(ulid));
        });
  }

  @Test
  void backsOffForAnApplicationConverter() {
    final var own = new UlidConverter();

    this.runner
        .withBean(UlidConverter.class, () -> own)
        .run(context -> assertSame(own, context.getBean(UlidConverter.class)));
  }
}
