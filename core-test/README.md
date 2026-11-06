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

- **`MsgIdSourceScanner`** (`io.repsy.core.test.msgid`, RPS-2516): the regular expressions, source
  walk and constant resolution behind the msgId scans of OS `MessageKeysTest`, Cloud
  `MessageIdsCoverageTest` and Imentis `MessageKeysTest`. `MsgIdSourceScanner.builder().build()`
  recognises the literal or `UPPER_SNAKE` constant that is the first argument of
  `success`/`warning`/`error` on a response factory and of a msgId-carrying exception;
  `exceptionPrefixes`, `responseReceivers` and `extraPattern` (a pattern ending in
  `LITERAL_OR_CONSTANT`, or declaring the named groups `literal`/`constant`) adapt it. `findSourceRoots`
  and `readSources` read the sources, `collectConstants` and `scan` return the used ids, the
  unresolved constants and the free-text or built exception ids. The product test keeps what is
  its own: the source directories, the bundle, the pending lists and every assertion, so a change to
  a pattern here shows up as a changed result there instead of a looser guard.

## Dependencies

`testcontainers-postgresql` (version through the `testcontainers-bom` import, the same version
`core-dependencies` imports), `junit-jupiter-api`, `assertj-core`, `logback-classic`, `slf4j-api`,
`spring-web`. The JDBC driver comes from the consumer.
