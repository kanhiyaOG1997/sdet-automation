# infra

Infrastructure for running the SUT and test suites locally and in CI.

| Directory | Contents |
|---|---|
| `docker/` | Docker Compose definitions (SUT, PostgreSQL, RabbitMQ, Kafka, Selenium Grid) |
| `kubernetes/` | Local cluster configuration and manifests (Phase 9) |

Integration tests start their own dependencies with Testcontainers; Compose is for running
the full stack interactively. All configuration is Docker-compatible so it runs unchanged on
OrbStack locally and on GitHub Actions runners
([ADR-0005](../docs/adr/0005-container-runtime-orbstack-testcontainers.md)).

Credentials are supplied through environment variables (see `.env.example` once added) and
are never committed.