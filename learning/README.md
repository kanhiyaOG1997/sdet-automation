# learning

Study material and practice code — **not** part of the production automation build.

| Directory | Topic |
|---|---|
| `java-core/` | Core Java language features (Java 21) |
| `collections-streams/` | Collections framework and Stream API |
| `oops-design-patterns/` | OOP principles and design patterns used in test frameworks |
| `interview-preparation/` | Interview notes and exercises: `java/`, `selenium/`, `api/` |

## Isolation rules

See [ADR-0003](../docs/adr/0003-learning-content-isolation.md).

- Nothing in `platform/`, `frameworks/`, `test-suites/` or `sut/` may depend on code here.
- Practice code is built only through an opt-in Maven profile, so it never slows down or
  breaks the main build.
- Quality gates still apply to Java code here, so examples model good practice.