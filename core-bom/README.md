# core-bom

Bill of materials (BOM) for the Repsy Core libraries.

## Purpose

Importing this BOM in a consuming project's `dependencyManagement` pins the versions of all
`io.repsy.core` modules to a single, consistent release, so downstream services don't have to
track individual module versions themselves.

The BOM's parent is `core-build-parent`: it manages the versions of the core modules (and the
Spring Boot BOM the library modules build on), not the application-only versions of
`core-dependencies`.

Modules covered:

- `core-event`
- `core-error-handling`
- `core-web-error`
- `core-web`
- `core-response`
- `core-ulid`
- `core-uuidv7`

## Usage

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

Then declare the individual modules you need without a `<version>`:

```xml
<dependency>
  <groupId>io.repsy.core</groupId>
  <artifactId>core-ulid</artifactId>
</dependency>
```
