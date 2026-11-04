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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

/** Starts a real container; skipped on a machine without Docker. */
class PostgresContainersDockerTest {

  @Test
  void startsAContainerWithTheGivenSettingsAndCreatesAnEmptyDatabase() throws Exception {
    assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is not reachable");

    try (final var container =
        PostgresContainers.start("app", "app", "app123", "max_connections=50")) {
      try (final var connection =
              DriverManager.getConnection(
                  container.getJdbcUrl(), container.getUsername(), container.getPassword());
          final var statement = connection.createStatement();
          final var result = statement.executeQuery("SHOW max_connections")) {
        result.next();
        assertThat(result.getString(1)).isEqualTo("50");
      }

      final var url = PostgresContainers.createDatabase(container);

      try (final var connection =
              DriverManager.getConnection(url, container.getUsername(), container.getPassword());
          final var statement = connection.createStatement();
          final var result =
              statement.executeQuery(
                  "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'")) {
        result.next();
        assertThat(result.getInt(1)).isZero();
        assertThat(url).startsWith("jdbc:postgresql://").contains("/it_");
      }
    }
  }
}
