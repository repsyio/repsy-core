# core-test

Test support shared by the products that build on repsy-core (Repsy OS, Repsy Cloud, Imentis).
Use it from the **test** scope only; the module is in `core-bom`, so the version comes from the
imported BOM.

## Purpose

- **`PostgresContainers`** (`io.repsy.core.test.postgres`): starts the `postgres:18` Testcontainer of
  an integration-test JVM with `fsync` off and extra server settings
  (`PostgresContainers.start("repsy", "repsy", "repsy123", "max_connections=300")`), and creates an
  empty database next to it for a test that runs Flyway itself (`createDatabase(container)`). A
  product keeps a small `SharedPostgres` class with a static field holding the started container, so
  that it starts once per JVM and Testcontainers' Ryuk reaper removes it at exit. Docker must be
  reachable: a missing daemon fails the class instead of skipping it.
- **`LogLeakRecorder`** and **`LogLeakFixtures`** (`io.repsy.core.test.logging`): a JUnit extension
  that records every log event of the classes under test at `TRACE`, with
  `assertNothingLogged(secret)` (fails when nothing was logged, or when a message or stack trace
  carries the secret) and `assertLoggedMessageContaining(text)`; plus failures whose messages,
  bodies and headers embed a request value. Register it as
  `@RegisterExtension final LogLeakRecorder log = LogLeakRecorder.watching(MyAdvice.class);`
  (RPS-2174).

The msgId scans of the products (`MessageKeysTest`, `MessageIdsCoverageTest`) are not here: they
read each product's own source tree and bundle.

## Dependencies

`testcontainers-postgresql` (version through the `testcontainers-bom` import, the same version
`core-dependencies` imports), `junit-jupiter-api`, `assertj-core`, `logback-classic`, `slf4j-api`,
`spring-web`. The JDBC driver comes from the consumer.
