# ADR-0004: Build our own system under test — SDET Testing Playground

- **Status:** Accepted
- **Date:** 2026-11-05

## Context

API, database, contract, messaging, performance and resiliency tests need a target. Public
demo applications are outside our control: they change without notice, rate-limit, go
offline, and cannot expose a database, message broker or fault injection.

## Decision

Build a Java 21 Spring Boot application, **SDET Testing Playground**, in `sut/`, implemented
incrementally:

1. A simple business domain with REST APIs on PostgreSQL.
2. RabbitMQ integration.
3. Kafka integration.
4. Fault-injection hooks for resiliency testing.

The SUT imports the Spring Boot BOM (`spring-boot-dependencies`) instead of inheriting the
Spring Boot parent POM, so it shares the repository parent and quality gates. Test code never
depends on SUT internals; tests interact only through public interfaces (HTTP, database,
message brokers).

## Consequences

- Deterministic, versioned tests that run identically locally and in CI.
- Full control over data, events and failure modes.
- We maintain an application in addition to the tests; its scope stays deliberately small.

## Alternatives considered

- **Public demo sites/APIs:** zero build cost, but external, flaky and unable to support
  database, messaging or resiliency testing.
- **Third-party open-source application in a container:** realistic, but harder to extend
  for specific test scenarios.