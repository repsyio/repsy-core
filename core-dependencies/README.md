# core-dependencies

Third-party versions the Repsy applications build on, kept apart from the library modules'
build parent (`core-build-parent`).

- Version properties an application references in its own entries: `bouncycastle.version`,
  `guava.version`, `java-jwt.version`, `totp.version`, `spring-cloud.version`,
  `spring-modulith.version`, `testcontainers-bom.version`, `h2.version`.
- Managed in `dependencyManagement`: H2 (override of the Spring Boot BOM, RPS-1676) and the Spring
  Cloud, Spring Modulith and Testcontainers BOMs.

No module of this repository uses these. An application normally gets them through `core-parent`
(which inherits this module). It can also import this module as a BOM, which brings the managed
versions but not the properties.
