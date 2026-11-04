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
package io.repsy.core.test.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The parts of {@link PostgresContainers} that need no Docker. */
class PostgresContainersTest {

  @Test
  void theCommandTurnsFsyncOffAndPassesEachSettingWithItsOwnFlag() {
    assertThat(PostgresContainers.command()).containsExactly("postgres", "-c", "fsync=off");
    assertThat(PostgresContainers.command("max_connections=300", "shared_buffers=64MB"))
        .containsExactly(
            "postgres",
            "-c",
            "fsync=off",
            "-c",
            "max_connections=300",
            "-c",
            "shared_buffers=64MB");
  }

  @Test
  void aDatabaseNameIsAnIdentifierWithoutDashes() {
    final var id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    assertThat(PostgresContainers.databaseName(id))
        .isEqualTo("it_123e4567e89b12d3a456426614174000");
  }

  @Test
  void theJdbcUrlNamesHostPortAndDatabase() {
    assertThat(PostgresContainers.jdbcUrl("localhost", 5432, "it_a"))
        .isEqualTo("jdbc:postgresql://localhost:5432/it_a");
  }

  @Test
  void theImageIsTheOneOfProduction() {
    assertThat(PostgresContainers.IMAGE).isEqualTo("postgres:18");
  }
}
