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

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts the one PostgreSQL container of an integration-test JVM.
 *
 * <p>A product keeps a {@code SharedPostgres} class with a static field that holds the result of
 * {@link #start}, so the container starts once, in the static initializer, and is left to
 * Testcontainers' Ryuk reaper to remove when the JVM exits. Starting it per class ({@code
 * &#64;Container}) would leave every cached Spring context pointing at a stopped database.
 *
 * <p>Docker must be reachable: a missing daemon fails the class with Testcontainers' message rather
 * than skipping it, so a CI run that lost its Docker socket goes red.
 */
public final class PostgresContainers {

  /** The image of production. */
  public static final String IMAGE = "postgres:18";

  private PostgresContainers() {}

  /**
   * Starts a {@value #IMAGE} container with {@code fsync} off, which is safe for a test database
   * and much faster.
   *
   * @param database the name of the database the container creates
   * @param username the user
   * @param password the password
   * @param settings further {@code name=value} server settings, for example {@code
   *     max_connections=300} (every cached Spring context keeps its own connection pool, and the
   *     default of 100 runs out once about ten of them are cached)
   * @return the started container
   */
  public static PostgreSQLContainer start(
      final String database,
      final String username,
      final String password,
      final String... settings) {
    // The container lives as long as the test JVM and Testcontainers' Ryuk reaper removes it at
    // exit: it is handed to the caller, so it is not closed here.
    final var container =
        new PostgreSQLContainer(IMAGE) // NOSONAR java:S2095
            .withDatabaseName(database)
            .withUsername(username)
            .withPassword(password)
            .withCommand(command(settings));

    container.start();

    return container;
  }

  /**
   * Creates an empty database next to the container's own, for a test that runs Flyway itself.
   *
   * @param container a started container
   * @return the JDBC URL of the new database
   */
  public static String createDatabase(final PostgreSQLContainer container) {
    final var name = databaseName(UUID.randomUUID());

    try (final var connection =
            DriverManager.getConnection(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
        final var statement = connection.createStatement()) {
      // An identifier cannot be a bind parameter; the name is "it_" and 32 hex digits of a random
      // UUID, never input.
      statement.execute("CREATE DATABASE " + name); // NOSONAR java:S2077
    } catch (final SQLException e) {
      throw new IllegalStateException("Could not create the database " + name, e);
    }

    return jdbcUrl(
        container.getHost(), container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT), name);
  }

  static String[] command(final String... settings) {
    final List<String> command = new ArrayList<>(List.of("postgres", "-c", "fsync=off"));

    for (final var setting : settings) {
      command.add("-c");
      command.add(setting);
    }

    return command.toArray(String[]::new);
  }

  static String databaseName(final UUID id) {
    return "it_" + id.toString().replace("-", "");
  }

  static String jdbcUrl(final String host, final int port, final String database) {
    return "jdbc:postgresql://%s:%d/%s".formatted(host, port, database);
  }
}
