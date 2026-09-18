# AGENTS.md

Guidance for AI coding agents working in this repository.

## What this repository is

`repsy-core` is a Maven multi-module project publishing shared Java libraries consumed by other
Repsy services: application events, error-handling/exceptions, REST response envelopes, and
time-sortable entity id generators (ULID and UUIDv7). See the root `README.md` and each module's
own `README.md` for details on what it does.

## Modules

| Module | Purpose |
| --- | --- |
| `core-parent` | Shared Maven configuration (Java version, quality-gate plugins, dependency versions) |
| `core-bom` | Dependency-management BOM importing all publishable modules |
| `core-event` | Shared Spring application events |
| `core-error-handling` | Common exceptions and error-reporting utilities |
| `core-response` | REST response envelope and factory |
| `core-ulid` | ULID entity-id generation/conversion (Hibernate) |
| `core-uuidv7` | UUIDv7 entity-id generation (Hibernate) |

`core-parent` is the Maven parent of every other module (the root `core` aggregator's parent is
`core-parent` too). Module dependency direction is one-way: `core-bom` references the others, and
the others don't depend on each other.

## Requirements

- Java 25 (enforced by `maven-enforcer-plugin`, range `[25,26)`)
- Maven 3.9.7+

## Build & verify

Always run from the repository root:

```bash
mvn verify
```

`mvn verify` runs the full quality gate, not just tests:

- JUnit tests (Surefire), UTC timezone forced
- JaCoCo coverage check — **minimum 80% instruction coverage** per module (`core-parent`
  `pom.xml`); a module failing this fails the build
- Checkstyle (`config/checkstyle.xml`)
- SpotBugs static analysis
- `fmt-maven-plugin` (Google Java Format) — code must already be formatted; the plugin checks, it
  does not auto-fix
- Apache RAT license-header check
- Error Prone runs as a javac plugin during compilation

To iterate faster while writing code, `-DskipTests` skips tests (and therefore the coverage
check) but still runs the other checks. Before considering a change done, run a full
`mvn verify`.

## Code style

- Format with `fmt-maven-plugin` (Google Java Format) — run `mvn com.spotify.fmt:fmt-maven-plugin:format`
  if `verify` reports formatting violations, rather than hand-formatting.
- Lombok and MapStruct annotation processors are available in every module via `core-parent`.
- Nullability follows JSpecify's package-level convention: every leaf package has a
  `package-info.java` annotated `@org.jspecify.annotations.NullMarked`, making all types in that
  package non-null by default. Only mark exceptions explicitly with `@Nullable`; don't add
  `@NonNull` — it's redundant under `@NullMarked` (see `core-response`, `core-ulid`,
  `core-error-handling`). Add a `package-info.java` with `@NullMarked` to any new package.
- New source files need the Apache 2.0 license header (see any existing file for the exact
  format) — the RAT plugin fails the build otherwise.
- Follow `config/checkstyle.xml` for style rules; it's enforced at `verify`, not just advisory.

## Testing

- Each module with source has a `src/test/java` counterpart; tests use JUnit Jupiter and Mockito
  (both provided by `core-parent`).
- Keep instruction coverage at or above 80% per module — this is a hard gate, not a suggestion.

## Releases

- Versioning/tagging is handled by `maven-release-plugin` (configured in the root `pom.xml`,
  tag format `v@{project.version}`) via the `.github/workflows/release.yml` pipeline. Don't bump
  versions by hand as part of unrelated changes.

## Adding a new module

1. Create the module directory with its own `pom.xml`, parented on `core-parent` (or on `core` if
   it needs to be listed as a Maven submodule — see the existing modules for the pattern).
2. Add it to `<modules>` in the root `pom.xml`.
3. If other services should be able to depend on it via `core-bom`, add it to `core-bom`'s
   `dependencyManagement`.
4. Add a `README.md` to the new module describing its purpose, following the style of the
   existing module READMEs.
5. Update the module table in the root `README.md` and in this file.
