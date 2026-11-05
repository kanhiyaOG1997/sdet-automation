# test-suites

Executable TestNG suites. Each suite depends on one or more `frameworks/` modules, targets
the [SDET Testing Playground](../sut/) and can be run on its own.

| Module | Scope | Phase |
|---|---|---|
| `api-tests` | REST API functional tests | 2 |
| `db-tests` | Data integrity and persistence assertions | 3 |
| `ui-selenium-tests`, `ui-playwright-tests` | UI end-to-end tests | 4 |
| `messaging-tests` | RabbitMQ and Kafka event flows | 5 |
| `contract-tests` | Consumer/provider contract tests | 6 |
| `accessibility-tests` | Automated accessibility checks | 6 |
| `security-tests` | Baseline security scanning | 6 |
| `mobile-tests` | Appium mobile tests | 8 |

Suites are executed in the Maven `integration-test` phase so `./mvnw test` stays fast and
infrastructure-free.