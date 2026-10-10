# core-build-parent

Shared Maven build configuration for the Repsy Core library modules.

## Purpose

This is the Maven parent of every library module. It centralizes:

- **Dependency versions** — Spring Boot (imported) and the versions the library modules use. The
  versions only applications use are in `core-dependencies`.
- **Compiler settings** — Java 25 source/target, Error Prone as a compiler plugin, and annotation
  processors for Lombok and MapStruct.
- **Quality gates**, enforced during `mvn verify`:
  - `jacoco-maven-plugin` — test coverage report and an 80% instruction-coverage check
    (`jacoco.check.phase`, default `verify`)
  - `maven-checkstyle-plugin` — style checks against `config/checkstyle.xml` of the directory Maven was started in
    (`checkstyle.config.location`)
  - `spotbugs-maven-plugin` — static analysis
  - `fmt-maven-plugin` — Google Java Format check
  - `maven-enforcer-plugin` — requires Maven >= 3.9.7 and Java 25
  - `apache-rat-plugin` — license header check
- **Baseline dependencies** — Lombok (compile), JUnit Jupiter and Mockito (test).

## Properties a consumer can set

| Property | Default | Meaning |
| --- | --- | --- |
| `test.jvm.args` | empty | Extra JVM arguments of every forked test JVM. It is appended to the `argLine` of Surefire (configured here) and of Failsafe (managed here; a consumer declares the plugin and its executions), after JaCoCo's agent and `-Duser.timezone=UTC`. Set it in the consumer's `<properties>` (for example `-XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=256m`) or with `-Dtest.jvm.args=...`. Do not set `argLine` itself for this: Maven would resolve `${argLine}` before JaCoCo's `prepare-agent` runs and the coverage would be lost silently. |

## Related parents

- `core-dependencies` (child of this one): the third-party versions applications use.
- `core-parent` (child of `core-dependencies`): what `repsy` (through the `core` aggregator) and
  `repsy-mono` use as their parent.

## Usage

Every library module in this repository declares `core-build-parent` as its Maven `<parent>`. You
shouldn't normally need to depend on it directly from outside this repository — consume the
individual `core-*` modules (optionally via `core-bom`) instead.
