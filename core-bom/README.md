# core-bom

Bill of materials (BOM) for the Repsy Core libraries.

## Purpose

Importing this BOM in a consuming project's `dependencyManagement` pins the versions of all
`io.repsy.core` modules to a single, consistent release, so downstream services don't have to
track individual module versions themselves.

Modules covered:

- `core-event`
- `core-error-handling`
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
