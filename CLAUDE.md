# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

Java 21 SDET automation portfolio: Maven multi-module reactor containing shared libraries
(`platform/`), automation frameworks (`frameworks/`), executable suites (`test-suites/`), the
system under test "SDET Testing Playground" (`sut/`), performance tests (`performance/`),
infrastructure (`infra/`) and isolated study material (`learning/`). See `README.md`,
`docs/adr/` and the current phase plan in `docs/plans/`.

Implemented so far (Phase 1): root POM, `platform/pom.xml` layer parent,
`platform/test-core` (configuration and secrets) and `platform/test-reporting` (Allure and
masking), `build-config/checkstyle-suppressions.xml`, the CI workflow and Dependabot. Other
module directories contain only READMEs.

## Commands

- Always use `./mvnw`, never a global `mvn`.
- Full gate, exactly as CI runs it: `./mvnw -B -ntp clean verify`
- Build one module with its dependencies: `./mvnw -pl <module-path> -am verify`
- Fast test loop without the quality gates: `./mvnw test`
- Fix formatting: `./mvnw spotless:apply` (check only: `./mvnw spotless:check`)
- Use `clean` whenever results feed an Allure report; Allure never deletes old results.

## Rules

- Java 21 only. All dependency and plugin versions live in the root `pom.xml`; child POMs
  never declare versions. The build enforces this (Enforcer `evaluateBeanshell`), and
  `banDynamicVersions` allows SNAPSHOTs only for the reactor's own modules (ADR-0007).
- New versions follow the 14-day release-age policy; no pre-releases. google-java-format is
  maintained manually, because Dependabot is not expected to update it (ADR-0007).
- Dependency direction: `test-suites → frameworks → platform`. Frameworks never depend on
  each other. Nothing depends on `learning/`. Tests touch `sut/` only through public
  interfaces (HTTP, database, brokers). Inside `platform`: `test-reporting → test-core`.
- Spotless (google-java-format) owns formatting; do not add formatting rules elsewhere.
  Checkstyle, SpotBugs and JaCoCo failures are fixed in the code, never by weakening a gate or
  adding suppressions without an approved reason.
- Coverage: platform production code needs line ≥ 80% and branch ≥ 70% (JaCoCo, D3). Every
  platform module with `src/main/java` must have `src/test/java` (R18). Write behavioural
  tests, not coverage padding.
- No credentials, tokens or real hostnames in code or config. Read secrets from environment
  variables; document them in `.env.example`.
- Secrets come only from `SECRET_*` environment variables, read through
  `TestConfig.requireSecret` (ADR-0008). Never put `secret.*` keys in YAML, never pass
  `-Dsecret.*` (Surefire XML records it; treat such a secret as exposed), and in CI map secrets
  through `env:` only. Never use `Secret.reveal()` in logs, reports or assertion messages. Use
  fake, deterministic values in tests.
- Reporting (ADR-0009): attach through `ReportAttachments`, never `Allure.attachment(...)`,
  whose content is not masked. Never pass secrets as data-provider parameters. Consuming
  modules depend on `test-reporting`, never on `allure-testng` directly, and keep
  `AllureLifecycleInstaller` first in the TestNG service file.
- No `Thread.sleep` in tests; use explicit waits or Awaitility.
- Containers are started through Testcontainers and must be Docker-compatible (no
  OrbStack-specific config).
- Add a dependency or abstraction only when current code needs it.
- Significant decisions get an ADR in `docs/adr/` (copy `0000-template.md`) and an entry in
  `docs/adr/README.md`.
- Every major module has a `README.md` covering purpose and how to run it.

## Workflow

- Create short-lived branches from `main` and merge back via pull request; there is no
  `develop` branch.
- Never commit, push, open a pull request, merge, rebase or amend without explicit approval.
  Approval for one step does not cover the next.
- Work on one approved task at a time and stay within its scope. Report deviations, and
  decisions that need the user, instead of deciding them. Do not change unrelated files.
- Report changed files, commands run and validation results after each task, with the actual
  diff and command output.
