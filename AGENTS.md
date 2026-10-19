# AGENTS.md

Guidance for AI coding agents working in this repository.

## What this repository is

`repsy-core` is a Maven multi-module project providing shared Java libraries consumed by other
Repsy services: application events, error-handling/exceptions, REST response envelopes, and
time-sortable entity id generators (ULID and UUIDv7). See the root `README.md` and each module's
own `README.md` for details on what it does.

## Modules

| Module | Purpose |
| --- | --- |
| `core-build-parent` | Maven parent of the library modules (Java version, quality-gate plugins, test dependencies) |
| `core-dependencies` | Third-party versions only applications use (properties, Spring Cloud/Modulith/Testcontainers BOMs, H2) |
| `core-parent` | Parent of the applications (`repsy`, `repsy-mono`): `core-build-parent` plus `core-dependencies`, no content of its own |
| `core-bom` | BOM listing all publishable modules (parent `core-build-parent`, so the root `mvn apache-rat:check` finds the RAT plugin on the first reactor project) |
| `core-event` | Shared Spring application events |
| `core-error-handling` | Common exceptions (pure JDK plus JSpecify) |
| `core-web-error` | `RequestReport` and `ExceptionReport` for logging web errors, `ConstraintViolations`, `ProblemField`, deprecated `ErrorUtils` |
| `core-web` | Spring MVC/Data web utilities: paging (`PagingParameterInterceptor`, `StablePaging`, `OffsetPageRequest`, `SortValidator`), `ResponseSizeLimitInterceptor`, `ResponseEntities`, `NoStore`, `LikePatterns`, `PutBodyPreservingFormContentFilter`, `XmlMapperConfig` |
| `core-response` | REST response envelope and factory |
| `core-ulid` | ULID entity-id generation/conversion (Hibernate) |
| `core-uuidv7` | UUIDv7 entity-id generation (Hibernate) |

`core-build-parent` is the Maven parent of every library module. The root `core` aggregator's
parent is `core-parent` (the chain `core-parent` -> `core-dependencies` -> `core-build-parent`),
because `repsy` uses `core` as its parent and `repsy-mono` uses `core-parent`; keep those
coordinates and the version properties they reference (`bouncycastle.version`, `guava.version`,
`java-jwt.version`, `totp.version`, `mapstruct.version`, `spring-boot.version`, ...) resolving.
Module dependency direction is one-way: `core-bom` references the others, and the others don't
depend on each other. Set `jacoco.check.phase` (default `verify`) to `none` to switch the
coverage gate off in a consumer; `checkstyle.config.location` defaults to
`${maven.multiModuleProjectDirectory}/config/checkstyle.xml`.

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
- JaCoCo coverage check — **minimum 80% instruction coverage** per module (`core-build-parent`
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
- Lombok and MapStruct annotation processors are available in every module via `core-build-parent`.
- Nullability follows JSpecify's package-level convention: every leaf package has a
  `package-info.java` annotated `@org.jspecify.annotations.NullMarked`, making all types in that
  package non-null by default. Only mark exceptions explicitly with `@Nullable`; don't add
  `@NonNull` — it's redundant under `@NullMarked` (see `core-response`, `core-ulid`,
  `core-error-handling`). Add a `package-info.java` with `@NullMarked` to any new package.
  NullAway (Error Prone, JSpecify mode, `OnlyNullMarked`, ERROR) checks every `@NullMarked`
  package at compile time and fails the build on a finding (RPS-2077, RPS-2151); a module that
  overrides `compilerArgs` must repeat its flags. Checkstyle (`config/checkstyle.xml`) fails a main
  package with types but no `package-info.java` (`JavadocPackage`), and a `package-info.java`
  without `@NullMarked`.
  The root `lombok.config` sets `lombok.addLombokGeneratedAnnotation = true`, so NullAway treats
  Lombok-generated members as unannotated code (RPS-2151).
  Consumers (RPS-2237, checked 2026-10-14 against `main` a658f0e, `-Xmaxwarns 100000`, `verify
  -DskipTests`). A core bump turns NullAway to ERROR and the `package-info` guard on for every
  project below the `core-parent` chain; none of them needs a change today:

  | Consumer | Parent chain | Core version | NullAway findings | `package-info` findings | How it keeps building |
  | --- | --- | --- | --- | --- | --- |
  | `repsy` (Repsy OS) | `core-parent` | release | 0 | 0 | Migrated in RPS-2151 |
  | `repsy-mono` (Cloud) | `core-parent` | release | 0 | 0 | Migrated in RPS-2151 |
  | `repsy-mono/imentis/imentis-backend` | `imentis` -> `repsy-mono` -> `core-parent` | release | 0 | 0 | `@NullMarked` packages (RPS-2150, RPS-2223 dropped its suppression) |
  | `repsy-mono/ext/*` (accounting, mail, payment, support, soft-delete, logging) | `ext` -> `repsy-mono` -> `core-parent` | release | 0 | 0 | `@NullMarked` packages |
  | `repsy/repsy-scanner-trivy` | Spring Boot parent, not `core-parent` | none | n/a | n/a | Carries its own Error Prone and NullAway flags (RPS-2150); a core bump changes nothing |

  Every consumer takes a released version (RPS-2265, RPS-2266), so a bump is a deliberate commit
  in the consumer, not something that follows `main`. No other project builds on `core-parent` or
  `core-build-parent` (the other `repsyio` repositories and `~/Projects` checkouts use the Spring
  Boot parent or are not Java). Before bumping a consumer, build it against the new commit: a new
  project that inherits `core-parent` must mark its packages `@NullMarked` and fix the findings, or
  set `-Xep:NullAway:WARN` in its own `compilerArgs` and record it here.
- New source files need the Apache 2.0 license header (see any existing file for the exact
  format) — the RAT plugin fails the build otherwise.
- Follow `config/checkstyle.xml` for style rules; it's enforced at `verify`, not just advisory.

## Testing

- Each module with source has a `src/test/java` counterpart; tests use JUnit Jupiter and Mockito
  (both provided by `core-build-parent`).
- Keep instruction coverage at or above 80% per module — this is a hard gate, not a suggestion.

## Merging to `main`

Two PRs can each pass CI against an older `main`, merge without a textual conflict, and still break
the build together (in `repsy`, RPS-901 and RPS-904 did: an unused import failed Checkstyle on
`main` and then on every open PR). There is **no merge queue**: `main` is protected by required
checks only. After other PRs merged first, update your branch from `main` and let the checks re-run
before you merge.

- Merge with `gh pr merge <n> --auto --squash` (or the "Merge when ready" button) once the required
  checks pass.
- `.github/workflows/pr-checks.yml` runs on `pull_request` only.
- Required checks in the `protect default` ruleset: `Java Core Lib check`,
  `Editorconfig check - All` and `PR title` (`pr-title.yml`). Add a new job to that list when it
  should gate merges.
- Every PR title reads `RPS-1234: Description`, or `RPS-1, RPS-2: Description` for several
  tickets. `pr-title.yml` checks it; Dependabot PRs and the PRs of the `repsy-release-bot` App
  (RPS-2268) are exempt. The squash commit takes the title.
- `gh pr merge --admin` skips the required checks. Org admins keep that bypass as a break-glass for
  a red `main` only. A PR merged that way is not re-verified against the other open PRs, so do not
  use it for routine merges.
- `repsy` and `repsy-mono` consume a released version of this repository (RPS-2265, RPS-2266); they
  never follow `main`. Bump a consumer only to a version that exists on repo.repsy.io (see
  "Releases").

## Dependabot and code scanning

- SonarCloud (PR analysis in `pr-checks.yml`; `main` is analysed by `sonarcloud.yml` at 10, 12, 14, 16 and 18 o'clock Europe/Amsterdam, not on every merge) is the only code scanner. There is no CodeQL workflow and GitHub code scanning is not configured; do not add them back (RPS-1849).
- `.github/dependabot.yml` checks daily. Minor and patch updates share one PR per update entry (group `minor-and-patch`); a major update gets its own PR. Every entry keeps `open-pull-requests-limit: 3`.
- The same rules apply in `repsy` and `repsy-mono`; change them in all three repositories together.


## Releases

- Versioning/tagging is handled by `maven-release-plugin` (configured in the root `pom.xml`,
  tag format `v@{project.version}`) via the `.github/workflows/release.yml` pipeline. Don't bump
  versions by hand as part of unrelated changes.
- Run a release with the **Release | Core** workflow (`workflow_dispatch`, or
  `gh workflow run release.yml`). `main` requires a pull request and neither the workflow token nor
  the `repsy-release-bot` App is a bypass actor of the `protect default` ruleset, so the workflow
  never pushes to `main`:
  1. `release:prepare` commits the release version and then the next development version, and
     tags the release commit `v<version>`, all locally.
  2. The workflow pushes those commits to a `release/v<version>` branch, together with the tag, and
     deploys the tag (see below).
  3. It opens a `Release <version>` pull request with the App token and stops. Auto-merge is off
     for the repository and the App cannot merge, so a person merges the pull request once its
     checks are green (an organization admin may use `gh pr merge --admin --squash`).
  4. Merging that pull request moves `main` to the next development version. The tag stays on the
     release commit, whose parent is the `main` commit the workflow started from. The squash merge
     gives `main` a new commit, so the tagged commit is reachable through the tag, not through
     `main`'s history.
- The pull request is opened with a token of the `repsy-release-bot` GitHub App (RPS-2268), not
  `github.token`: a pull request opened with `github.token` triggers no `pull_request` workflows,
  so its required checks would never report. The App has Contents and Pull requests write, no
  webhook, and is installed on `repsy-core`, `repsy` and `repsy-mono` only; its ID is the org
  variable `REPSY_BOT_APP_ID` and its private key the org secret `REPSY_BOT_APP_PRIVATE_KEY`
  (both visible to those three repositories). A first step fails the run, before anything is
  tagged, when either is missing. Its pull requests skip the PR title check, so they need no Jira
  key.
- If the release pull request can't merge, delete the `release/v<version>` branch and the
  `v<version>` tag (`git push origin --delete release/v<version> v<version>`) before running the
  workflow again; `release:prepare` fails when the tag already exists. If the deploy has already
  published the version, cut the next patch version instead (see below).
- A release is a tag and a deployment (RPS-2264, which replaced the tag-only rule of RPS-1085).
  After pushing the branch and the tag, the workflow checks the tagged commit out into a separate
  worktree and runs `mvn deploy` there, which publishes every module (parents, BOMs and libraries)
  to the public `repsy-maven` repository, `https://repo.repsy.io/repsy/repsy-maven`, named by the
  `distributionManagement` of `core-build-parent`. The credentials are the `REPSY_MAVEN_USERNAME`
  and `REPSY_MAVEN_TOKEN` Actions secrets of this repository; a first step fails the run before
  anything is tagged when either is missing. Consumers read the repository without credentials.
  Maven Central stays out until a public consumer needs it (it would need POM metadata and
  source/javadoc/signing plugins).
- Versions follow SemVer, picked by whoever dispatches the release (the version `release:prepare`
  asks for), and the whole reactor shares one version. A change that removes or alters a public
  class, method or auto-configured bean is a major bump, a backwards-compatible addition is a
  minor one, anything else a patch. Downstream builds use released versions only, never
  `-SNAPSHOT`.
- A published release version cannot be replaced. If the deploy step fails part-way, some modules
  may already be in the repository: delete them there (or cut the next patch version) as well as
  the `release/v<version>` branch and the `v<version>` tag before running the workflow again.
- `repsy` and `repsy-mono` consume the release (RPS-2265, RPS-2266): the version of their parent
  `core-parent` and, once more, `repsy-core.version` in their root `pom.xml` (the `core-bom` import),
  resolved from the public repository through a `<repositories>` entry, which Maven reads even for
  the parent. The `v<version>` tag names the release; the tagged commit is not on `main`'s history. A
  bump is a deliberate change in the consumer after the release exists on repo.repsy.io. The last
  step of the release workflow sends `core-released` (the version) to `repsyio/repsy` with the App
  token, which opens the bump pull request there and, once a person merged it, in `repsy-mono`
  (RPS-2269); a person merges both. No `-SNAPSHOT` of this repository is used downstream.
- Things a consumer must do because of how `core-parent` is published: set
  `checkstyle.config.location` itself and keep a copy of `config/checkstyle.xml` (`core-parent` sets
  it to the relative `../../config/checkstyle.xml` of its own layout); write its own `<version>` in
  the root `pom.xml` (it used to inherit it from the parent); keep the two version places equal
  (`CoreVersionPinTest` in `repsy`, a `pr-checks` step in `repsy-mono` that also compares the OS
  submodule).
- A `io.repsy.core:*:1.0.0` installed locally from an older checkout shadows the release (Maven
  prefers a local release to the remote) and gives confusing errors, such as a missing
  `spring-modulith` version. Delete `~/.m2/repository/io/repsy/core/*/1.0.0`, or build once with
  `-Dmaven.repo.local=<empty dir>`, which is what CI sees.

## Adding a new module

1. Create the module directory with its own `pom.xml`, parented on `core-build-parent` (see the
   existing modules for the pattern).
2. Add it to `<modules>` in the root `pom.xml`.
3. If other services should be able to depend on it via `core-bom`, add it to `core-bom`'s
   `dependencyManagement`.
4. Add a `README.md` to the new module describing its purpose, following the style of the
   existing module READMEs.
5. Update the module table in the root `README.md` and in this file.
