# ADR-0002: Maven multi-module layout with centralized versions

- **Status:** Accepted
- **Date:** 2026-11-05

## Context

The repository will hold several automation frameworks (API, Selenium, Playwright, Appium),
their test suites, shared utilities and a system under test. These must share versions and
quality gates, yet each automation module must be runnable independently.

## Decision

1. **One Maven reactor** with a root POM acting as both parent and aggregator.
2. **Layered modules** with a one-way dependency rule:

   ```
   test-suites/*  →  frameworks/*  →  platform/*
   sut/           →  (no dependency on test code)
   ```

   Frameworks never depend on each other. The rule will be enforced by an automated
   architecture test once code exists.
3. **Centralized versions:** all dependency and plugin versions are declared once in the
   root POM (`dependencyManagement`, `pluginManagement`, upstream BOM imports). Child modules
   never declare versions.
4. **Java 21 only**, enforced by the Maven Enforcer plugin.
5. **Maven Wrapper is mandatory**: `./mvnw` is the only supported entry point, pinned to a
   specific Maven version (script-only wrapper, so no binary is committed).
6. **Independent execution:** any module and its dependencies can be built and tested alone,
   e.g. `./mvnw -pl test-suites/api-tests -am verify`.
7. A module is created only when it has real code; no placeholder modules.

## Consequences

- One command builds and verifies everything; version conflicts surface in one place.
- Heavy suites (UI, mobile, performance) need opt-in profiles to keep the default build fast.
- Selenium, Appium and Playwright bring overlapping transitive dependencies; dependency
  convergence must be managed explicitly in the root POM.

## Alternatives considered

- **One repository per framework:** stronger isolation, but duplicated build configuration
  and no single place to show how the pieces fit together.
- **Gradle:** capable, but Maven was chosen as the project standard and is more common in
  enterprise QA environments.