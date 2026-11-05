# sut — SDET Testing Playground

The system under test for every suite in this repository: a Java 21 Spring Boot application
owned by this project, so tests run against a deterministic, versioned target rather than
third-party demo sites. See [ADR-0004](../docs/adr/0004-own-system-under-test.md).

## Incremental scope

| Increment | Capability | Enables |
|---|---|---|
| 1 | Simple business domain with REST APIs on PostgreSQL | API, database, performance testing |
| 2 | RabbitMQ integration | Messaging and resiliency testing |
| 3 | Kafka integration | Event-streaming testing |
| 4 | Fault-injection hooks | Resiliency testing |

The SUT imports the Spring Boot BOM rather than using the Spring Boot parent POM, so it
shares the repository's parent POM and quality gates.