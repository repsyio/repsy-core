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
| `core-parent` | Shared Maven configuration (Java version, quality-gate plugins, dependency versions) |
| `core-bom` | Dependency-management BOM importing all publishable modules |
| `core-event` | Shared Spring application events |
| `core-error-handling` | Common exceptions (pure JDK plus JSpecify) |
| `core-web-error` | `RequestReport` and `ExceptionReport` for logging web errors, `ConstraintViolations`, `ProblemField`, deprecated `ErrorUtils` |
| `core-web` | Spring MVC/Data web utilities: paging (`PagingParameterInterceptor`, `StablePaging`, `OffsetPageRequest`, `SortValidator`), `ResponseSizeLimitInterceptor`, `ResponseEntities`, `NoStore`, `LikePatterns`, `PutBodyPreservingFormContentFilter`, `XmlMapperConfig` |
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
  NullAway (Error Prone, JSpecify mode, `OnlyNullMarked`, currently WARN) checks every `@NullMarked`
  package at compile time (RPS-2077); a module that overrides `compilerArgs` must repeat its flags.
- New source files need the Apache 2.0 license header (see any existing file for the exact
  format) — the RAT plugin fails the build otherwise.
- Follow `config/checkstyle.xml` for style rules; it's enforced at `verify`, not just advisory.

## Testing

- Each module with source has a `src/test/java` counterpart; tests use JUnit Jupiter and Mockito
  (both provided by `core-parent`).
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
- Required checks in the `main-branch-protection` ruleset: `Java Core Lib check`,
  `Editorconfig check - All` and `PR title` (`pr-title.yml`). Add a new job to that list when it
  should gate merges.
- Every PR title reads `RPS-1234: Description`, or `RPS-1, RPS-2: Description` for several
  tickets. `pr-title.yml` checks it; Dependabot PRs are exempt. The squash commit takes the title.
- `gh pr merge --admin` skips the required checks. Org admins keep that bypass as a break-glass for
  a red `main` only. A PR merged that way is not re-verified against the other open PRs, so do not
  use it for routine merges.
- `repsy` consumes this repository as its `core/` submodule. Only bump that pointer to a commit
  that is on this repository's `main`: a commit that only exists on a PR branch disappears from
  the remote when the branch is deleted on merge.

## Dependabot and code scanning

- SonarCloud (PR analysis in `pr-checks.yml`; `main` is analysed by `sonarcloud.yml` at 10, 12, 14, 16 and 18 o'clock Europe/Amsterdam, not on every merge) is the only code scanner. There is no CodeQL workflow and GitHub code scanning is not configured; do not add them back (RPS-1849).
- `.github/dependabot.yml` checks daily. Minor and patch updates share one PR per update entry (group `minor-and-patch`); a major update gets its own PR. Every entry keeps `open-pull-requests-limit: 3`.
- The same rules apply in `repsy` and `repsy-mono`; change them in all three repositories together.


## Releases

- Versioning/tagging is handled by `maven-release-plugin` (configured in the root `pom.xml`,
  tag format `v@{project.version}`) via the `.github/workflows/release.yml` pipeline. Don't bump
  versions by hand as part of unrelated changes.
- Run a release with the **Release | Core** workflow (`workflow_dispatch`, or
  `gh workflow run release.yml`). `main` requires a pull request and the workflow token is not a
  bypass actor of the `main-branch-protection` ruleset, so the workflow never pushes to `main`:
  1. `release:prepare` commits the release version and then the next development version, and
     tags the release commit `v<version>`, all locally.
  2. The workflow pushes those commits to a `release/v<version>` branch, together with the tag, and
     opens a `Release <version>` pull request that it merges with auto-merge (squash).
  3. Merging that pull request moves `main` to the next development version. The tag stays on the
     release commit, whose parent is the `main` commit the workflow started from. The squash merge
     gives `main` a new commit, so the tagged commit is reachable through the tag, not through
     `main`'s history.
- The workflow needs **Allow GitHub Actions to create and approve pull requests** turned on in the
  repository's Actions settings, or `gh pr create` is refused.
- Pull requests opened with the workflow token don't trigger `pull_request` workflows. While
  `pr-checks.yml` checks are required, the release pull request gets no result and can't merge;
  open it with a GitHub App token instead of `github.token` if that blocks the release.
- If the pull request can't merge, delete the `release/v<version>` branch and the `v<version>` tag
  (`git push origin --delete release/v<version> v<version>`) before running the workflow again;
  `release:prepare` fails when the tag already exists.
- A release is a tag, nothing more. Nothing builds or deploys the tagged version: there is no
  `distributionManagement`, no `deploy` or `release:perform` step, and no artifact on repo.repsy.io
  or Maven Central (decided in RPS-1085). `release:prepare` only runs `clean verify` locally to
  check the release commit. Every consumer (`repsy`, and the private `repsy-mono`) vendors this
  repository as a git submodule and builds it with `mvn install -f core/pom.xml`. The
  `v<version>` tag names a released version; the tagged commit is not on `main`'s history, so a
  consumer still pins a commit on `main` (see "Merging to `main`"), normally the release pull
  request's squash commit or a later one.
- The release pull request moves `main` to the next development version, so a consumer that bumps
  its submodule pointer past it must bump `<parent><version>` in its root `pom.xml` in the same
  change: Maven ignores `relativePath` when the parent version does not match.
- Revisit publishing (repo.repsy.io first, Central only with a public consumer) when a consumer
  cannot use the submodule. The prerequisites are listed in RPS-1085: a checkout-independent
  Checkstyle config location in `core-parent`, POM metadata (`name`, `description`, `url`,
  `licenses`, `developers`) and source/javadoc/signing plugins for Central, and registry
  credentials as Actions secrets in this repository.

## Adding a new module

1. Create the module directory with its own `pom.xml`, parented on `core-parent` (or on `core` if
   it needs to be listed as a Maven submodule — see the existing modules for the pattern).
2. Add it to `<modules>` in the root `pom.xml`.
3. If other services should be able to depend on it via `core-bom`, add it to `core-bom`'s
   `dependencyManagement`.
4. Add a `README.md` to the new module describing its purpose, following the style of the
   existing module READMEs.
5. Update the module table in the root `README.md` and in this file.
