# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

Java 21 SDET automation portfolio: Maven multi-module reactor containing shared libraries
(`platform/`), automation frameworks (`frameworks/`), executable suites (`test-suites/`), the
system under test "SDET Testing Playground" (`sut/`), performance tests (`performance/`),
infrastructure (`infra/`) and isolated study material (`learning/`). See `README.md` and
`docs/adr/`.

## Commands

- Always use `./mvnw`, never a global `mvn`.
- Fix formatting: `./mvnw spotless:apply`
- Build one module with its dependencies: `./mvnw -pl <module-path> -am verify`

## Rules

- Java 21 only. All dependency and plugin versions live in the root `pom.xml`; child POMs
  never declare versions.
- Dependency direction: `test-suites → frameworks → platform`. Frameworks never depend on
  each other. Nothing depends on `learning/`. Tests touch `sut/` only through public
  interfaces (HTTP, database, brokers).
- Spotless (google-java-format) owns formatting; do not add formatting rules elsewhere.
- No credentials, tokens or real hostnames in code or config. Read secrets from environment
  variables; document them in `.env.example`.
- No `Thread.sleep` in tests; use explicit waits or Awaitility.
- Containers are started through Testcontainers and must be Docker-compatible (no
  OrbStack-specific config).
- Add a dependency or abstraction only when current code needs it.
- Significant decisions get an ADR in `docs/adr/` (copy `0000-template.md`).
- Every major module has a `README.md` covering purpose and how to run it.

## Workflow

- Create short-lived branches from `main` and merge back via pull request; there is no
  `develop` branch. Never commit or push without explicit approval.
- Report changed files, commands run and validation results after each task.