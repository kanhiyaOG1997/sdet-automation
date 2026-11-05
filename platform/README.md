# platform

Shared libraries used by every framework and test suite. They have **no dependency on any
browser driver, HTTP client or mobile driver**, so they stay small and fast to test.

| Module (planned) | Responsibility | Phase |
|---|---|---|
| `test-core` | Typed configuration, test context, logging, waits, retry policy | 1 |
| `test-reporting` | Allure integration, attachments, TestNG listeners | 1 |
| `test-data` | Test data builders and fixture loading | 2 |
| `db-client` | JDBC access and query helpers for database assertions | 3 |
| `messaging-client` | RabbitMQ and Kafka producers/consumers for event assertions | 5 |

Each module is unit tested and coverage-gated with JaCoCo. A module is added only when a
framework or suite needs it.