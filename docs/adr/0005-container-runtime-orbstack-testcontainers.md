# ADR-0005: OrbStack locally, Testcontainers for integration tests

- **Status:** Accepted
- **Date:** 2026-11-05

## Context

Integration tests need PostgreSQL, RabbitMQ, Kafka and browsers. They must run the same way
on a macOS Apple Silicon workstation and on GitHub Actions Linux runners.

## Decision

- **OrbStack** is the local container runtime on macOS Apple Silicon. It exposes a standard
  Docker API and socket, so tooling needs no OrbStack-specific configuration.
- **Testcontainers** starts and stops each integration test's dependencies from Java code.
- All container configuration (images, Compose files) stays **Docker-compatible**, using
  images that publish both `arm64` and `amd64` variants.
- No test or build file may reference OrbStack directly.

## Consequences

- The same tests run unchanged locally and in CI (where Docker is preinstalled on Ubuntu
  runners).
- Integration tests require a running container engine; unit tests do not.
- Image versions are pinned to avoid drift between environments.

## Alternatives considered

- **Docker Desktop:** heavier on Apple Silicon and requires a licence in larger organizations.
- **Colima:** viable, but needs extra Testcontainers socket configuration.
- **Shared long-lived test environments:** non-hermetic and prone to cross-test interference.