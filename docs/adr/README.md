# Architecture Decision Records

New ADRs copy [`0000-template.md`](0000-template.md) and take the next number.

| ADR | Title | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-maven-multi-module-layout.md) | Maven multi-module layout with centralized versions | Accepted |
| [0003](0003-learning-content-isolation.md) | Keep learning content isolated under `learning/` | Accepted |
| [0004](0004-own-system-under-test.md) | Build our own SUT — SDET Testing Playground | Accepted |
| [0005](0005-container-runtime-orbstack-testcontainers.md) | OrbStack locally, Testcontainers for integration tests | Accepted |
| [0006](0006-code-quality-toolchain.md) | Code quality toolchain without duplicate formatting rules | Accepted |
| [0007](0007-build-and-quality-gates.md) | Enforce build rules and quality gates in layered parent POMs | Accepted |
| [0008](0008-test-configuration-and-secrets.md) | Layered test configuration with environment-only secrets | Accepted |
| [0009](0009-reporting-allure-and-masking.md) | Allure 3 reporting with masking at write time | Accepted |

Implementation plans live in [`../plans/`](../plans/), for example the
[Phase 1 plan](../plans/phase-1.md).
