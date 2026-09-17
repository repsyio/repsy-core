# core-parent

Shared Maven configuration for the Repsy Core multi-module build.

## Purpose

This is the top-level Maven parent for every `core-*` module (and the root `core` aggregator).
It centralizes:

- **Dependency versions** — third-party libraries (Spring Boot, Spring Cloud, Spring Modulith,
  Jackson, Testcontainers, and various one-off libraries like Guava, Stripe, Mailjet, etc.) via
  `dependencyManagement` and version properties.
- **Compiler settings** — Java 25 source/target, Error Prone as a compiler plugin, and annotation
  processors for Lombok and MapStruct.
- **Quality gates**, enforced during `mvn verify`:
  - `jacoco-maven-plugin` — test coverage report and an 80% instruction-coverage check
  - `maven-checkstyle-plugin` — style checks against `../config/checkstyle.xml`
  - `spotbugs-maven-plugin` — static analysis
  - `fmt-maven-plugin` — Google Java Format check
  - `maven-enforcer-plugin` — requires Maven >= 3.9.7 and Java 25
  - `apache-rat-plugin` — license header check
- **Baseline dependencies** — Lombok (compile), JUnit Jupiter and Mockito (test).

## Usage

Every other module in this repository declares `core-parent` as its Maven `<parent>`. You
shouldn't normally need to depend on it directly from outside this repository — consume the
individual `core-*` modules (optionally via `core-bom`) instead.
