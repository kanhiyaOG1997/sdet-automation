# ADR-0006: Code quality toolchain without duplicate formatting rules

- **Status:** Accepted
- **Date:** 2026-11-05

## Context

Production-grade code needs consistent formatting, style rules, bug detection and coverage
measurement, enforced by the build rather than by individual IDE settings. Running a
formatter and a style checker with overlapping formatting rules causes conflicting failures.

## Decision

Each tool has a single, non-overlapping responsibility, enforced in the Maven build:

| Tool | Responsibility |
|---|---|
| **Spotless + google-java-format** | Sole owner of Java formatting (whitespace, indentation, imports, line wrapping). `spotless:apply` fixes; `spotless:check` gates. |
| **Checkstyle (Google conventions)** | Non-formatting rules only: naming, Javadoc on public API, imports hygiene, complexity. Formatting checks (indentation, line length, whitespace, wrapping) are disabled because Spotless owns them. |
| **SpotBugs** | Static bug detection. |
| **JaCoCo** | Coverage measurement, with minimum thresholds on library modules (`platform`, `frameworks`). |

The Checkstyle configuration lives in the repository and is the same one the IDE uses.
`.editorconfig` only covers editor defaults and non-Java files.

## Consequences

- One formatting authority; formatting failures are fixed by running `./mvnw spotless:apply`.
- The IntelliJ Checkstyle plugin must point at the repository configuration instead of its
  bundled Sun/Google configurations.
- Coverage thresholds apply to library code, not to test suites.

## Alternatives considered

- **Checkstyle alone for formatting:** detects but cannot fix formatting.
- **Spotless alone:** formats but enforces no naming or design rules.