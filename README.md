# SDET Automation Engineering

**Production-grade Java automation frameworks, quality engineering practices, and advanced SDET engineering solutions.**

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-Multi--Module-C71A36?logo=apachemaven)](https://maven.apache.org/)
[![Selenium](https://img.shields.io/badge/Selenium-4-43B02A?logo=selenium)](https://www.selenium.dev/)
[![Playwright](https://img.shields.io/badge/Playwright-Java-2EAD33?logo=playwright)](https://playwright.dev/java/)
[![RestAssured](https://img.shields.io/badge/REST-Assured-25A162)](https://rest-assured.io/)
[![GitHub Actions](https://img.shields.io/badge/CI-GitHub_Actions-2088FF?logo=githubactions)](https://github.com/features/actions)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 1. Project Overview

**SDET Automation Engineering** is a comprehensive Java-based automation engineering portfolio designed to demonstrate the skills, architecture, tools, and engineering practices expected from a Senior Software Development Engineer in Test (SDET-3).

This repository is not just a collection of automation scripts.

It is a unified engineering ecosystem containing:

- Reusable automation frameworks.
- API, UI, mobile and database testing solutions.
- Performance and security testing.
- Contract and integration testing.
- Microservices and event-driven testing.
- CI/CD and infrastructure automation.
- Test architecture and engineering documentation.
- A dedicated application under test: **SDET Testing Playground**.

The repository follows a Maven multi-module architecture, with centralized dependency management, reusable components, independent test execution and automated quality checks.

### Primary objectives

1. Design and implement scalable automation frameworks.
2. Demonstrate practical Java automation engineering skills.
3. Build reusable components following SOLID and clean code principles.
4. Test real-world distributed systems and microservices.
5. Integrate automation into CI/CD pipelines.
6. Demonstrate advanced debugging, reliability and performance engineering.
7. Document architectural decisions and engineering trade-offs.
8. Build a portfolio that demonstrates SDET-3-level technical ownership.

---

## 2. Architecture Overview

The repository follows a layered architecture with clear separation of responsibilities.

```text
                  SDET AUTOMATION ENGINEERING
                             |
             +---------------+---------------+
             |               |               |
          Platform        Frameworks      SUT
             |               |               |
       Shared Libraries  Automation      Spring Boot
             |            Engines        Application
             |               |
             +---------------+
                     |
                 Test Suites
                     |
          +----------+----------+
          |          |          |
         API        UI       Database
          |          |          |
       RestAssured Selenium  PostgreSQL
                  Playwright
                     |
             Test Execution
                     |
              Test Reporting
                     |
              CI/CD Pipeline
```

### Architectural layers

| Layer | Responsibility |
|---|---|
| Platform | Shared configuration, reporting, test data and infrastructure clients |
| Frameworks | Reusable API, Selenium, Playwright and mobile automation frameworks |
| Test Suites | Executable business, integration and end-to-end test scenarios |
| SUT | Application being tested |
| Performance | Load, stress and endurance testing |
| Infrastructure | Docker, Kubernetes and CI/CD configuration |
| Learning | Isolated study material and practice code ([ADR-0003](docs/adr/0003-learning-content-isolation.md)) |

### Dependency direction

Module dependencies flow one way ([ADR-0002](docs/adr/0002-maven-multi-module-layout.md)):

```text
Test Suites
     |
     v
Frameworks
     |
     v
Platform
```

- Frameworks never depend on each other.
- The System Under Test is independent of the automation code. Tests interact with it only
  through public interfaces (HTTP, database, message brokers), never through its internals
  ([ADR-0004](docs/adr/0004-own-system-under-test.md)).
- No production module depends on `learning/`.

This ensures reusability, maintainability and separation of concerns.

---

## 3. Repository Structure

```text
sdet-automation/
│
├── pom.xml                         # parent + aggregator, all versions          (Phase 1)
├── mvnw, mvnw.cmd, .mvn/wrapper/   # Maven Wrapper — the only build entry point
├── build-config/                   # Checkstyle suppressions                      (Phase 1)
├── README.md, CLAUDE.md, LICENSE
├── .gitignore, .gitattributes, .editorconfig
│
├── platform/                       # shared, framework-agnostic libraries (pom.xml = layer parent)
│   ├── test-core/                  # layered configuration, secrets               (Phase 1)
│   ├── test-reporting/             # Allure integration, masking, TestNG listeners (Phase 1)
│   ├── test-data/                  # test data builders, fixtures                 (Phase 2)
│   ├── db-client/                  # JDBC access for database assertions          (Phase 3)
│   └── messaging-client/           # RabbitMQ and Kafka clients                   (Phase 5)
│
├── frameworks/                     # reusable automation libraries
│   ├── api-framework/              # RestAssured                                  (Phase 2)
│   ├── selenium-framework/         # Selenium 4                                   (Phase 4)
│   ├── playwright-framework/       # Playwright Java                              (Phase 4)
│   └── appium-framework/           # Appium                                       (Phase 8)
│
├── test-suites/                    # executable TestNG suites
│   ├── api-tests/                                                                 (Phase 2)
│   ├── db-tests/                                                                  (Phase 3)
│   ├── ui-selenium-tests/                                                         (Phase 4)
│   ├── ui-playwright-tests/                                                       (Phase 4)
│   ├── messaging-tests/                                                           (Phase 5)
│   ├── contract-tests/                                                            (Phase 6)
│   ├── accessibility-tests/                                                       (Phase 6)
│   ├── security-tests/                                                            (Phase 6)
│   └── mobile-tests/                                                              (Phase 8)
│
├── sut/                            # SDET Testing Playground (Spring Boot)        (Phase 2)
│
├── performance/
│   └── jmeter/                     # load, stress and endurance plans             (Phase 7)
│
├── infra/
│   ├── docker/                     # Docker Compose for the local stack
│   └── kubernetes/                 # local cluster, Selenium Grid                 (Phase 9)
├── Jenkinsfile                                                                    (Phase 9)
│
├── learning/                       # isolated study material (ADR-0003)
│   ├── java-core/
│   ├── collections-streams/
│   ├── oops-design-patterns/
│   └── interview-preparation/
│
├── docs/
│   ├── adr/                        # Architecture Decision Records
│   ├── plans/                      # phase implementation plans                   (Phase 1)
│   ├── architecture/
│   ├── test-strategy/
│   └── system-design/
│
└── .github/
    ├── workflows/ci.yml            # GitHub Actions build                         (Phase 1)
    └── dependabot.yml              # dependency and action updates                (Phase 1)
```

**Note:** This is the target architecture. A module is created only when it contains real
code ([ADR-0002](docs/adr/0002-maven-multi-module-layout.md)); the phase shown is when it is
introduced. Not every directory is a Maven module — infrastructure, documentation and
performance assets use their own execution mechanisms.

**Implemented today (Phase 1):** the root POM, the `platform` layer parent and its two modules
[`platform/test-core`](platform/test-core/) and [`platform/test-reporting`](platform/test-reporting/),
`build-config/`, the CI workflow and Dependabot. Every other module directory listed above
contains at most a README.

---

## 4. Technology Stack

### Programming and build

| Technology | Purpose |
|---|---|
| Java 21 | Primary programming language |
| Maven | Build and dependency management |
| Maven Wrapper | Reproducible builds |
| TestNG | Test execution and orchestration |
| JDBC | Database connectivity |

### UI automation

| Technology | Purpose |
|---|---|
| Selenium 4 | Browser automation |
| Playwright Java | Modern browser automation |
| Appium | Mobile automation |
| Page Object Model | UI framework design |
| Selenium Grid | Distributed browser execution |

### API and integration testing

| Technology | Purpose |
|---|---|
| RestAssured | REST API automation |
| Jackson | JSON serialization and deserialization |
| JSON Schema Validator | Response contract validation |
| Pact JVM | Consumer-driven contract testing |
| WireMock | Service virtualization |

### Database and messaging

| Technology | Purpose |
|---|---|
| PostgreSQL | Relational database testing |
| JDBC | Database interaction |
| Testcontainers | Containerized integration testing |
| RabbitMQ | Message queue testing |
| Apache Kafka | Event streaming and messaging |

### Performance, security and accessibility

| Technology | Purpose |
|---|---|
| Apache JMeter | Load and performance testing |
| OWASP ZAP | Security testing |
| axe-core | Accessibility testing |

Code quality tooling is described in [Code Quality and Engineering Standards](#8-code-quality-and-engineering-standards).

### DevOps and infrastructure

| Technology | Purpose |
|---|---|
| Docker | Containerization |
| OrbStack | Local container runtime on macOS ([ADR-0005](docs/adr/0005-container-runtime-orbstack-testcontainers.md)) |
| Kubernetes | Container orchestration |
| Jenkins | CI/CD |
| GitHub Actions | Automated workflows |
| Allure | Test reporting |
| SLF4J + Logback | Logging |

Dependencies and versions are centrally managed in the root Maven POM; child POMs never declare
versions, and the build enforces this ([ADR-0007](docs/adr/0007-build-and-quality-gates.md)).

Tools will be introduced when their corresponding modules are implemented. Phase 1 uses Java 21,
Maven, TestNG, AssertJ, Jackson, SLF4J with Logback (tests only), Allure and GitHub Actions; the
rest of this stack is planned.

---

## 5. SDET Testing Playground – System Under Test

A major component of this repository is a custom Java Spring Boot application called **SDET Testing Playground**.

The objective is to build a controlled application that acts as the target for our automation frameworks.

Instead of depending entirely on public demo applications, we will own the application and control its data, business rules, APIs and failure scenarios.

### Proposed business capabilities

| Module | Functionalities |
|---|---|
| User Management | Registration, authentication and authorization |
| Loan Management | Loan creation, approval and rejection |
| Payment Management | Payment processing and transaction status |
| Notification | Event-driven notifications |
| Reporting | Transaction and operational reports |
| Administration | User roles, permissions and configuration |

### Proposed architecture

```text
                 SDET TESTING PLAYGROUND
                           |
                     REST APIs
                           |
                    Spring Boot
                           |
              +------------+------------+
              |            |            |
           Users         Loans       Payments
              |            |            |
              +------------+------------+
                           |
                     PostgreSQL
                           |
              +------------+------------+
              |                         |
           RabbitMQ                    Kafka
              |                         |
       Async Processing            Event Streaming
```

### Testing opportunities

The application will support:

- API functional testing.
- Authentication and authorization testing.
- Database validation.
- Transaction and data integrity testing.
- Message queue validation.
- Event-driven architecture testing.
- Contract testing.
- End-to-end testing.
- Performance and load testing.
- Failure recovery and resiliency testing.

The SUT will be implemented incrementally ([ADR-0004](docs/adr/0004-own-system-under-test.md)):

1. REST APIs on PostgreSQL.
2. RabbitMQ integration.
3. Kafka integration.
4. Fault-injection hooks for resiliency testing.

It imports the Spring Boot BOM rather than inheriting the Spring Boot parent POM, so it shares
this repository's parent POM and quality gates. See [`sut/`](sut/).

---

## 6. Getting Started

### Prerequisites

| Tool | Requirement |
|---|---|
| Git | Installed |
| JDK | Java 21 (the build enforces `[21,22)`; CI uses Eclipse Temurin 21) |
| IntelliJ IDEA | Recommended IDE |
| Maven | Not required — use the bundled Maven Wrapper (`./mvnw`, Maven 3.10.0) |
| Container runtime | OrbStack or any Docker-compatible runtime, required for integration tests (from Phase 2; not needed in Phase 1) |
| GitHub | Repository access |

### Clone the repository

```bash
git clone git@github-qa:kanhiyaOG1997/sdet-automation.git

cd sdet-automation
```

The SSH host alias above assumes the repository owner has configured the corresponding GitHub SSH identity.

### Configure Java

Verify your Java installation:

```bash
java -version
javac -version
```

The supported Java language level is 21.

### Verify Maven Wrapper

Always build with the wrapper, never a global `mvn`:

```bash
./mvnw -version
```

### Build the project

```bash
./mvnw clean verify
```

This is the complete gate and the same command CI runs: Enforcer rules, compilation (warnings
are errors), unit tests, then Spotless, Checkstyle, SpotBugs and JaCoCo. Use `clean` whenever
an Allure report will be generated: Allure never deletes old results.

### Execute a specific module

Build a module together with the modules it depends on:

```bash
./mvnw -pl platform/test-core verify
./mvnw -pl platform/test-reporting -am verify
```

### Execute tests

```bash
./mvnw test                  # compile and run unit tests, without the quality gates
./mvnw spotless:apply        # fix formatting before verify
```

Phase 1 has unit tests only. Integration suites (Failsafe, Testcontainers) arrive in Phase 2.
Additional module-specific commands and execution profiles will be documented as the frameworks
become available.

### Configuration and secrets

Test configuration is layered YAML plus environment variables and system properties
([`platform/test-core`](platform/test-core/README.md)). Secrets come **only** from `SECRET_*`
environment variables, never from YAML or `-Dsecret.*`. A secret passed with `-D` is copied
into Surefire XML reports and must be treated as exposed and rotated
([ADR-0008](docs/adr/0008-test-configuration-and-secrets.md)).

---

## 7. Automation Framework Engineering Standards

All automation frameworks will follow consistent engineering principles.

### Framework design

- Page Object Model for UI automation.
- Reusable API request specifications.
- Centralized environment configuration.
- Independent and reusable test components.
- Test data management.
- Separation of test logic and business logic.
- Consistent logging and reporting.
- Configurable browser and environment selection.

### Test design

Every relevant test suite should cover:

1. Positive scenarios.
2. Negative scenarios.
3. Boundary-value scenarios.
4. Equivalence partitioning.
5. Authentication and authorization.
6. Data integrity.
7. Error handling.
8. Regression scenarios.
9. Integration scenarios.
10. Concurrency and resiliency where applicable.

### Test reliability

- Avoid hardcoded waits.
- Prefer explicit synchronization.
- Avoid unnecessary test dependencies.
- Keep tests independent and repeatable.
- Use deterministic test data.
- Capture useful failure evidence.
- Avoid blind retries that hide genuine defects.
- Make parallel execution safe.

---

## 8. Code Quality and Engineering Standards

The repository follows maintainable software engineering practices. Each tool has one,
non-overlapping responsibility and is enforced by the Maven build
([ADR-0006](docs/adr/0006-code-quality-toolchain.md)).

| Tool | Responsibility |
|---|---|
| Spotless 3.10.4 + google-java-format 1.36.1 | Sole owner of Java formatting; fix with `./mvnw spotless:apply` |
| Checkstyle 14.3.0 (Google conventions) | Non-formatting rules only: naming, Javadoc, imports, coding rules. Formatting checks are suppressed in [`build-config/checkstyle-suppressions.xml`](build-config/checkstyle-suppressions.xml) |
| SpotBugs 4.10.5 | Static bug analysis (effort `Max`, threshold `Medium`) |
| JaCoCo 0.8.15 | Coverage on every module; **line ≥ 80% and branch ≥ 70%** enforced on `platform` production code. Thresholds for `frameworks` are decided in Phase 2 |

All gates run in `verify`; the Maven Enforcer rules (Java 21, pinned plugins, no dynamic or
child-POM versions, dependency convergence) run in `validate`
([ADR-0007](docs/adr/0007-build-and-quality-gates.md)). Every version follows a 14-day
release-age policy.

IDE plugins such as SonarQube for IDE may be used for development-time feedback, but they are
not build gates.

### Coding principles

- SOLID principles.
- Clean Code.
- DRY where appropriate.
- KISS.
- Meaningful naming conventions.
- Small, focused methods.
- Reusable components without unnecessary abstractions.
- Explicit exception handling.
- Consistent logging.
- Meaningful assertions.
- Appropriate design patterns.

### Architecture principles

- Separation of concerns.
- High cohesion.
- Low coupling.
- Dependency inversion.
- Modular design.
- Centralized dependency management.
- Independent test execution.
- Clear module ownership and boundaries.

---

## 9. CI/CD and Execution Strategy

Automation will progressively integrate with GitHub Actions and Jenkins.

### Current pipeline (Phase 1)

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on pull requests to `main`, pushes
to `main` and manual dispatch:

- One job, **"Build and verify"**, on `ubuntu-24.04` with Temurin 21 and a 15-minute timeout,
  running `./mvnw --batch-mode --no-transfer-progress --fail-at-end verify`.
- Read-only token (`contents: read`), actions pinned by full commit SHA, and
  `persist-credentials: false`. Uses `pull_request`, not `pull_request_target`.
- If the Maven step itself fails, a follow-up step builds the JaCoCo report from the recorded
  coverage data.
- Unless the run is cancelled, Surefire, JaCoCo, Allure-results, Checkstyle and SpotBugs reports
  are uploaded as the `build-reports` artifact (14 days).
- A newer push cancels a running PR build. On `main`, a running build is never cancelled, and
  GitHub keeps at most one pending run per branch.
- `main` is protected by the repository ruleset `Protect main`: every change needs a pull
  request, and the required status check `Build and verify` (shown in pull requests as
  "CI / Build and verify") must pass on a branch that is up to date with `main`. Force pushes
  and deleting `main` are blocked.

[`.github/dependabot.yml`](.github/dependabot.yml) proposes weekly, grouped updates for Maven
and GitHub Actions, with a 14-day cooldown. google-java-format is not expected to be covered,
because it is referenced only inside the Spotless configuration. The first Dependabot run
proposed no google-java-format update, which supports this but does not conclusively confirm
it, so it is still maintained manually ([ADR-0007](docs/adr/0007-build-and-quality-gates.md)).

### Planned pipeline

```text
Developer Push
      |
      v
Pull Request
      |
      v
GitHub Actions
      |
      +----> Compile
      |
      +----> Static Analysis
      |
      +----> Unit Tests
      |
      +----> API Tests
      |
      +----> Integration Tests
      |
      +----> UI Tests
      |
      +----> Test Reports
      |
      v
Build Validation
      |
      v
Merge Approval
```

### Planned pipeline capabilities

- Automated compilation.
- Unit and integration test execution.
- API automation.
- Browser automation.
- Parallel test execution.
- Test result reporting.
- Code quality checks.
- Dependency vulnerability scanning.
- Docker-based test environments.
- Build artifact management.

Pipeline stages will be introduced incrementally rather than requiring every environment for every pull request.

---

## 10. Test Reporting and Observability

Every framework should provide meaningful execution visibility.

Planned reporting capabilities:

- Test execution summary.
- Passed, failed and skipped test counts.
- Failure messages and stack traces.
- Request and response logging where appropriate.
- Screenshots for UI failures.
- Browser traces.
- Execution duration.
- Environment information.
- Historical execution trends.

Allure will be the primary test reporting solution.

Sensitive information such as access tokens, passwords and personal data must be redacted from logs and reports.

**Implemented in Phase 1** ([`platform/test-reporting`](platform/test-reporting/README.md),
[ADR-0009](docs/adr/0009-reporting-allure-and-masking.md)):

- Allure 3.0.0 for TestNG, with raw results in `target/allure-results`. Report generation and
  publishing come in Phase 2.
- Configured secrets and values of sensitive keys are masked before content is written. This
  covers attachments made through `ReportAttachments`, environment information, log lines, test
  results, containers and run-level errors.
- **Not masked:** attachment *content* written directly through Allure (`Allure.attachment(...)`,
  Allure's HTTP filters, global attachments; only their names are masked), images, encoded
  secrets, and Surefire XML reports.
- Never pass secrets as data-provider parameters: Allure's `historyId` is derived from them.
- If listener order would let Allure write unmasked errors, the run fails instead.

---

## 11. Security Guidelines

Security is a core engineering requirement.

### Credential management

- Never commit credentials or access tokens.
- Use environment variables for local configuration. Test secrets are read only from `SECRET_*`
  environment variables; YAML files and `-Dsecret.*` system properties are rejected
  ([ADR-0008](docs/adr/0008-test-configuration-and-secrets.md)).
- Use GitHub Actions Secrets for CI/CD credentials, mapped through the workflow's `env:` block,
  never on the Maven command line.
- Keep `.env` files out of Git.
- Add `.env.example` when the first local secret configuration is needed. It contains
  placeholder values only, never real secrets. Real secrets stay outside version control and
  are supplied as `SECRET_*` environment variables.
- Never commit private SSH keys or certificates.

### Test data protection

- Do not commit production customer data.
- Use synthetic or anonymized test data.
- Redact sensitive information from API logs.
- Sanitize HAR files before sharing or committing.
- Avoid exposing authentication cookies or session tokens.

### Dependency security

- Review third-party dependencies.
- Monitor known vulnerabilities.
- Avoid unnecessary dependencies.
- Prefer maintained and trusted libraries.

Security vulnerabilities should be reported privately to the repository owner rather than disclosed publicly.

---

## 12. Git Workflow and Contribution Guidelines

We follow trunk-based development with short-lived branches.

### Branch strategy

```text
main  (protected, always releasable)
  |
  +---- feature/api-framework       --> pull request --> main
  |
  +---- feature/selenium-framework  --> pull request --> main
  |
  +---- bugfix/test-data-issue      --> pull request --> main
```

- Every branch is created from `main` and merged back into `main` through a pull request.
- Branches stay small and short-lived — ideally merged within a few days.
- There is no long-lived `develop` branch.

### Branch naming conventions

| Branch type | Convention |
|---|---|
| Feature | `feature/<feature-name>` |
| Bug fix | `bugfix/<issue-name>` |
| Hotfix | `hotfix/<issue-name>` |
| Documentation | `docs/<documentation-name>` |
| Refactoring | `refactor/<component-name>` |

### Commit message convention

Follow Conventional Commits.

Examples:

```text
feat(api): add authentication support

test(ui): add login regression scenarios

fix(framework): resolve browser lifecycle issue

refactor(config): centralize environment properties

docs(architecture): add API framework ADR

chore(build): configure Maven wrapper
```

### Pull request expectations

Every pull request should:

1. Explain the purpose of the change.
2. Reference related issues where applicable.
3. Include relevant test coverage.
4. Pass required build and quality checks.
5. Include documentation updates where necessary.
6. Avoid unrelated changes.
7. Be reviewed before merging.

No direct changes to `main` without the required review and validation.

---

## 13. Architecture Decision Records

Significant technical decisions are recorded as Architecture Decision Records in
[`docs/adr/`](docs/adr/) ([ADR-0001](docs/adr/0001-record-architecture-decisions.md)).
New ADRs copy [`0000-template.md`](docs/adr/0000-template.md), which has these sections:

- Context — the problem and the constraints.
- Decision — what was decided.
- Consequences — trade-offs and follow-up work.
- Alternatives considered — and why each was rejected.

| ADR | Decision |
|---|---|
| [0001](docs/adr/0001-record-architecture-decisions.md) | Record architecture decisions |
| [0002](docs/adr/0002-maven-multi-module-layout.md) | Maven multi-module layout with centralized versions |
| [0003](docs/adr/0003-learning-content-isolation.md) | Keep learning content isolated under `learning/` |
| [0004](docs/adr/0004-own-system-under-test.md) | Build our own SUT — SDET Testing Playground |
| [0005](docs/adr/0005-container-runtime-orbstack-testcontainers.md) | OrbStack locally, Testcontainers for integration tests |
| [0006](docs/adr/0006-code-quality-toolchain.md) | Code quality toolchain without duplicate formatting rules |
| [0007](docs/adr/0007-build-and-quality-gates.md) | Enforce build rules and quality gates in layered parent POMs |
| [0008](docs/adr/0008-test-configuration-and-secrets.md) | Layered test configuration with environment-only secrets |
| [0009](docs/adr/0009-reporting-allure-and-masking.md) | Allure 3 reporting with masking at write time |

Implementation plans with their task history live in [`docs/plans/`](docs/plans/), for example
the [Phase 1 plan](docs/plans/phase-1.md).

---

## 14. Project Roadmap

The repository will be developed incrementally.

| Phase | Scope | Status |
|---|---|---|
| Phase 0 | Repository foundation, Git hygiene, documentation and Maven Wrapper | Done |
| Phase 1 | Maven architecture, quality gates, shared platform modules and CI | In progress ([plan](docs/plans/phase-1.md)) |
| Phase 2 | SDET Testing Playground v1: REST APIs, PostgreSQL and API framework | Planned |
| Phase 3 | Database automation and data integrity testing | Planned |
| Phase 4 | Selenium and Playwright automation frameworks | Planned |
| Phase 5 | RabbitMQ, Kafka and event-driven testing | Planned |
| Phase 6 | Contract, accessibility and security testing | Planned |
| Phase 7 | Performance and load testing with JMeter | Planned |
| Phase 8 | Mobile automation using Appium | Planned |
| Phase 9 | Docker, Kubernetes, Selenium Grid and Jenkins | Planned |
| Phase 10 | Architecture documentation, case studies and portfolio polish | Planned |

Progress and implementation details will be updated as each phase is completed.

---

## 15. Learning and Interview Preparation

The repository also serves as a technical learning and interview preparation resource.

Topics include:

- Java fundamentals and advanced Java.
- OOP and SOLID principles.
- Collections and Streams.
- Data structures and algorithms.
- Design patterns.
- Selenium and Playwright internals.
- API testing and HTTP protocols.
- SQL and database concepts.
- Microservices architecture.
- Distributed systems.
- Performance engineering.
- Test framework architecture.
- CI/CD and DevOps.
- SDET-3 system design.
- Real-world debugging scenarios.

Learning material lives in [`learning/`](learning/) and stays separate from production
automation modules ([ADR-0003](docs/adr/0003-learning-content-isolation.md)).

---

## 16. Development Environment

The primary development environment is:

| Component | Configuration |
|---|---|
| Operating System | macOS |
| Architecture | Apple Silicon |
| IDE | IntelliJ IDEA |
| AI Coding Assistant | Claude Code |
| Java | Eclipse Temurin 21 |
| Build Tool | Maven |
| Version Control | Git and GitHub |
| Container Runtime | OrbStack |
| CI/CD | GitHub Actions |

The repository will remain compatible with other supported development environments and Docker-compatible CI runners.

---

## 17. Project Status

This is an actively evolving engineering portfolio.

The repository is being developed phase by phase, with an emphasis on working implementations, meaningful test coverage, documentation and reproducible execution.

**The objective is to demonstrate engineering capability through actual implementation, not simply list technologies.**

---

## 18. License

This project is licensed under the MIT License.

See the [LICENSE](LICENSE) file for details.

---

**Built with Java, automation engineering principles and a focus on quality at scale.**