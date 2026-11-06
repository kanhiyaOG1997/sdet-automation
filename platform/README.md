# platform

Shared libraries used by every framework and test suite. They have **no dependency on any
browser driver, HTTP client or mobile driver**, so they stay small and fast to test.

| Module | Responsibility | Status |
|---|---|---|
| [`test-core`](test-core/) | Typed, layered configuration and environment-only secrets ([ADR-0008](../docs/adr/0008-test-configuration-and-secrets.md)) | Implemented (Phase 1) |
| [`test-reporting`](test-reporting/) | Allure integration, masked attachments, sensitive-data masking, TestNG logging listeners ([ADR-0009](../docs/adr/0009-reporting-allure-and-masking.md)) | Implemented (Phase 1) |
| `test-data` | Test data builders and fixture loading | Planned (Phase 2) |
| `db-client` | JDBC access and query helpers for database assertions | Planned (Phase 3) |
| `messaging-client` | RabbitMQ and Kafka producers/consumers for event assertions | Planned (Phase 5) |

Dependency direction inside the layer: `test-reporting → test-core`.

Waits, a shared test context and a retry policy were once planned for `test-core`. They are
added with their first consumer, in Phase 2–4 ([Phase 1 plan](../docs/plans/phase-1.md) §1).

## Build rules for this layer

`platform/pom.xml` (`platform-parent`) adds to the root POM's rules
([ADR-0007](../docs/adr/0007-build-and-quality-gates.md)):

- **Coverage gate:** JaCoCo line ≥ 80% and branch ≥ 70% of production code, per module (D3).
- **Tests required:** a module with `src/main/java` must have `src/test/java`, otherwise
  `validate` fails (R18). An empty test directory still passes this guard.

A module is added only when a framework or suite needs it.

```bash
./mvnw -pl platform/test-core verify
./mvnw -pl platform/test-reporting -am verify
```
