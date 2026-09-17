# Repsy Core

Shared Java libraries used across Repsy services. The project is organized as a Maven multi-module build and publishes reusable building blocks for events, API responses, error handling, and time-sortable identifiers.

## Modules

| Module | Purpose |
| --- | --- |
| `core-bom` | Dependency management for the Repsy Core modules |
| `core-event` | Shared Spring application events |
| `core-error-handling` | Common exceptions and error-handling utilities |
| `core-response` | REST response DTOs and response factories |
| `core-ulid` | ULID generation, conversion, and Hibernate support |
| `core-uuidv7` | UUIDv7 generation and Hibernate support |
| `core-parent` | Shared Maven configuration and dependency versions |

## Requirements

- Java 25
- Maven 3.9.7 or newer

## Build

Run the verification build from the repository root:

```bash
mvn verify
```

To skip tests when producing a local build:

```bash
mvn verify -DskipTests
```

## Using the libraries

Import the BOM in a consuming Maven project to keep Repsy Core module versions aligned:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.repsy.core</groupId>
      <artifactId>core-bom</artifactId>
      <version>1.0.0-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

Then add the individual modules your application needs, for example:

```xml
<dependency>
  <groupId>io.repsy.core</groupId>
  <artifactId>core-ulid</artifactId>
</dependency>
```

## License

Licensed under the Apache License, Version 2.0. See [LICENSE.txt](LICENSE.txt).
