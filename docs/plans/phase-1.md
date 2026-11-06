# Phase 1 Implementation Plan — Build Backbone

- **Status:** Revision 4 — reconciled with the implementation and approved. Closeout status
  2026-11-07: Tasks 1–8 are completed. Phase 1 was merged to `main` through PR #2 (merge commit
  `e0976ab`); the ruleset `Protect main` requires the `Build and verify` check; O3 Scenario A was
  validated on PR #3, which was closed without merging; Scenario B was not executed and is an
  accepted residual risk (R14, R22). **Phase 1 is not yet formally closed:** closure is pending the
  merge of the closeout documentation PR and a final verification. Every remaining step,
  including each commit, push and pull request, needs its own explicit approval (§15)
- **Date:** 2026-11-06 (versions and SHAs verified 2026-11-05; re-checked against the 2026-11-06
  cutoff — no selection changes; action SHAs re-verified 2026-11-06 in Task 6)
- **Branch:** `feature/phase-1-build-backbone`, created in Task 1 from `origin/main` at `cdebdd1`
- **Location:** `docs/plans/phase-1.md`. Until Task 7 this plan lived, uncommitted, at the
  repository root as `PHASE-1-PLAN.md`; it was never committed there, so it has no earlier Git
  history. Revisions 1–4 are summarised below
- **Related ADRs:** [0002](../adr/0002-maven-multi-module-layout.md),
  [0003](../adr/0003-learning-content-isolation.md),
  [0004](../adr/0004-own-system-under-test.md),
  [0006](../adr/0006-code-quality-toolchain.md)

### Revision 4 changes (Task 7)

Revision 4 records what Tasks 1–6 actually implemented and the decisions approved during their
reviews. It changes no approved decision, version, scope or acceptance criterion; items that
need a new decision are listed as open items in §18.

| Area | Change |
|---|---|
| Location | Moved from `PHASE-1-PLAN.md` to `docs/plans/phase-1.md`; relative ADR links updated |
| Decisions | D7–D10 record the decisions approved in the Task 5 reviews (§2) |
| Architecture | Layout, POM hierarchy and module dependencies match the implemented modules (§3) |
| Enforcer | `banDynamicVersions` allowlist, child-POM version rule and the R18 guard documented (§4.5) |
| `test-core` | Environment-only secrets (D7) and the Task 4 security findings S1–S4 (§7) |
| `test-reporting` | Implemented classes, Allure result and run-level-error masking, tests (§8) |
| CI and Dependabot | Implemented content, Task 6 validation results, main-branch concurrency wording corrected, Dependabot coverage limits (§11) |
| Risks | R1 corrected: google-java-format is not expected to be updated by Dependabot (not yet confirmed) and is maintained manually; R13 validated; R22 added (§17) |
| Status | Task status and actual commits (§15); open items O4–O6 (§18); status against the definition of done (§19) |
| Status update (2026-11-07) | Tasks 6, 7, 7a and 7b recorded as completed and committed, with their commits (§3.1, §11.2, §12, §13.4, §14, §15, §18, §19); Task 8 not started |
| Follow-up | O4 and O5 scheduled as separate Tasks 7a and 7b, after the Task 7 and Task 6 commits and before Task 8, in that order (7b only after 7a is completed and reviewed), each needing its own approval (§15, §18); neither is executed in Task 7 |
| Closeout update (2026-11-07) | Task 8 results recorded: local validation and O6 evidence (§8.6, §10); PR #2 merge and its process deviation (§15); required check `Build and verify` set through the ruleset `Protect main` (§11.3); O3 Scenario A results and Scenario B accepted as residual risk (§11.4, §16, R14, R22, O3); first Dependabot run (§11.5); definition-of-done status (§19). The rows above are kept as history. No technical decision, version or scope changes |

### Revision 3 changes

| Area | Change |
|---|---|
| Decisions | O1 approved (D5) and O2 deferred to Phase 2 (D6) — §2 |
| GitHub Actions | Maven step gets `id: verify`; the coverage-on-failure step runs only when `failure() && steps.verify.outcome == 'failure'`; linted with negative controls; behavioural validation plan with a scenario truth table (§11) |
| Allure results | Source-verified that Allure 3.0.0 defaults to `allure-results` **relative to the working directory**, i.e. outside `target/`; Surefire now sets the directory explicitly; the wiring test asserts the exact directory; stale-result and `clean` behaviour validated (§8.6, §10) |
| `.gitattributes` | Approved change specified exactly, with expected `git check-attr` results (§13) |
| Risks | R14–R16 added (§17) |

### Revision 2 changes

| Area | Change |
|---|---|
| Decisions | D1–D3 recorded as approved (§2). Allure selection re-verified and recorded with evidence (§5) |
| Checkstyle | Suppressions validated against the real Checkstyle 14.3.0 Google configuration and an empirical formatter probe; list corrected (§6) |
| Spotless | `reflowLongStrings` enabled — the probe showed the Spotless default conflicts with three Checkstyle rules (§6.3) |
| GitHub Actions | Actions pinned to verified full commit SHAs; workflow linted with actionlint; reports uploaded on failure, including coverage (§11) |
| `.gitattributes` | Technical analysis of the `*.har` rules; recommendation **revised** (§13) |
| Consistency | Layer-parent table corrected for ADR-0004; README structure update added; ADR-0006 coverage scope noted (§14) |
| Controls | Implementation controls made explicit (§16) |

---

## 1. Objective and scope

Phase 1 establishes the build backbone every later phase depends on: a root parent POM, a
platform layer parent, two real shared modules with meaningful tests, four enforced quality
gates, and continuous integration on every pull request.

### In scope

| # | Deliverable |
|---|---|
| 1 | Root Maven parent POM (Java 21), acting as parent and aggregator |
| 2 | Maven multi-module structure with a `platform` layer parent |
| 3 | Centralized dependency and plugin version management |
| 4 | Quality gates: Spotless (google-java-format), Checkstyle, SpotBugs, JaCoCo |
| 5 | `platform/test-core` — layered test configuration and secret handling |
| 6 | `platform/test-reporting` — Allure integration, sensitive-data masking, test logging |
| 7 | GitHub Actions CI workflow and Dependabot |
| 8 | Unit tests, positive and negative build validation |
| 9 | ADRs 0007–0009 and module documentation |

### Out of scope

| Item | Deferred to | Reason |
|---|---|---|
| Frameworks, SUT, test suites | Phase 2+ | ADR-0002 rule 7: a module exists only when it contains real code |
| Testcontainers, Failsafe execution | Phase 2 | The first integration suite arrives with the SUT |
| Allure report generation (`allure-maven`) and GitHub Pages | Phase 2 | Nothing worth publishing until real suites exist; version recorded in §5.3 |
| Waits / Awaitility, shared test context, retry policy | Phase 2–4 | Added with their first consumer — no speculative abstractions |
| ArchUnit dependency-direction tests (ADR-0002) | Phase 2 | Meaningful only once `frameworks/` exists |

### Starting state (verified 2026-11-05)

| Check | Result |
|---|---|
| `origin/main` | `cdebdd1` — merge of PR #1, contains Phase 0 |
| Local `main` | 2 commits behind `origin/main`; working tree clean apart from this plan |
| Root `pom.xml` | Does not exist |

---

## 2. Approved decisions

| # | Decision | Approved value | Consequence |
|---|---|---|---|
| D1 | Maven coordinates and package root | `groupId` **`io.github.kanhiyaog1997.sdet`** | Packages `io.github.kanhiyaog1997.sdet.core…`, `io.github.kanhiyaog1997.sdet.reporting…` |
| D2 | JDK range for building | **`[21,22)`** | Builds only on JDK 21 — identical locally and in CI |
| D3 | Coverage thresholds | **Line ≥ 80%, branch ≥ 70%** for **platform production modules only** | Enforced on `src/main` code of `platform/*` modules. Test code, `sut/`, `test-suites/` and `learning/` are never gated by this rule |
| D4 | Allure Java adapter | **3.0.0** — selected on evidence, see §5 | Recorded in ADR-0009 |
| D5 | `.gitattributes` HAR handling (O1) | **Approved:** keep `*.har text eol=lf diff`, remove `*.har binary`, correct the comment | HAR files stay reviewable as text; sanitization before commit remains mandatory (§13) |
| D6 | Coverage thresholds for `frameworks` (O2) | **Deferred to Phase 2** | ADR-0006 unchanged; thresholds evaluated when real framework modules exist |
| D7 | Secret sources (Task 5 review, finding F2, option C) | **`SECRET_*` environment variables only**; a `secret.*` system property makes configuration loading fail | Breaking change to Task 4 behaviour, committed separately (`d9691cb`). CI maps secrets through `env:` only (§11.3). Residual risk R21 |
| D8 | Masking of Allure results (Task 5 review, finding F1) | **`AllureResultMasker`** masks every text field of test results and containers in `beforeTestWrite` / `beforeContainerWrite`; fails closed; never changes identifiers or `historyId` | §8.3; the data-provider rule and direct-attachment limitation are documented in the module README |
| D9 | Masking of Allure run-level errors (Task 5 review, decision A) | **`MaskingResultsWriter`**, installed by **`AllureLifecycleInstaller`** through the public `Allure.setLifecycle` API before `AllureTestNg` is constructed; fails closed and aborts the run if the listener order is wrong | §8.3, R19. No reflection or internal Allure API |
| D10 | Internal SNAPSHOT versions (Task 5 review, decisions 2 and B) | **Exact `banDynamicVersions` allowlist** of `test-core` and `test-reporting`, plus an Enforcer rule that forbids dependency versions in child POMs | §4.5, R20. The allowlist is never widened to the whole group |

D7–D10 were approved in the Task 5 reviews under the labels shown in brackets and
are numbered here only so this plan can refer to them.

---

## 3. Module architecture

### 3.1 Directory layout after Phase 1

Status markers reflect the repository on 2026-11-07: **done** (committed). Every file listed
is committed; the task that added it is named where it is not one of Tasks 1–5.

```
sdet-automation/
├── pom.xml                                   # done — root parent + aggregator
├── build-config/
│   └── checkstyle-suppressions.xml           # done — disables formatting checks owned by Spotless
├── platform/
│   ├── pom.xml                               # done — platform layer parent (coverage gate, R18 guard)
│   ├── test-core/
│   │   ├── pom.xml                           # done
│   │   ├── README.md                         # done (Task 7a)
│   │   └── src/
│   │       ├── main/java/io/github/kanhiyaog1997/sdet/core/config/…        # done
│   │       ├── test/java/io/github/kanhiyaog1997/sdet/core/config/…        # done
│   │       └── test/resources/logback-test.xml                              # done
│   └── test-reporting/
│       ├── pom.xml                           # done
│       ├── README.md                         # done
│       └── src/
│           ├── main/java/io/github/kanhiyaog1997/sdet/reporting/…          # done
│           ├── main/resources/META-INF/services/                            # done
│           │   ├── org.testng.ITestNGListener
│           │   ├── io.qameta.allure.listener.TestLifecycleListener
│           │   └── io.qameta.allure.listener.ContainerLifecycleListener
│           ├── test/java/io/github/kanhiyaog1997/sdet/reporting/…          # done
│           └── test/resources/{config/default.yml,logback-test.xml}         # done
├── .github/
│   ├── workflows/ci.yml                      # done (Task 6)
│   └── dependabot.yml                        # done (Task 6)
└── docs/
    ├── plans/phase-1.md                      # done — this plan (Task 7)
    └── adr/
        ├── 0007-build-and-quality-gates.md       # done (Task 7a)
        ├── 0008-test-configuration-and-secrets.md # done (Task 7a)
        └── 0009-reporting-allure-and-masking.md  # done (Task 7a)
```

`build-config/` is a new top-level directory; the README repository structure was updated to
include it in Task 7a (§12).

### 3.2 POM hierarchy

```
pom.xml  (sdet-automation-parent)         versions, plugins, quality gates for ALL modules
 └── platform/pom.xml  (platform-parent)  adds coverage thresholds for platform production code
      ├── test-core                       depends on: jackson-databind, jackson-dataformat-yaml, slf4j-api
      └── test-reporting                  depends on: test-core, allure-testng, allure-java-commons,
                                                      testng, jackson-databind, slf4j-api
```

### 3.3 Architectural explanations

**Root POM is both parent and aggregator.** It is the single source of truth for versions and
plugin configuration (ADR-0002). It declares `<dependencyManagement>` but **no
`<dependencies>`**: inherited dependencies would leak into every module — the SUT would
receive TestNG, platform libraries would receive Selenium. Each module declares only what it
uses; the root decides only *which version* it gets.

**Per-layer build rules.** Each layer needs different rules, expressed declaratively by a thin
parent POM, without profiles or conditional logic:

| POM | Role | Specific rules | Introduced |
|---|---|---|---|
| `platform/pom.xml` | Layer parent | JaCoCo thresholds for platform production code (D3) | Phase 1 |
| `frameworks/pom.xml` | Layer parent | Framework conventions; coverage policy per ADR-0006, thresholds evaluated in Phase 2 (D6) | Phase 2 |
| `test-suites/pom.xml` | Layer parent | Failsafe execution; no coverage gate | Phase 2 |
| `sut/pom.xml` | **Module, not a layer parent** | Imports the Spring Boot BOM (ADR-0004) | Phase 2 |

Layer parents are created only when their first module exists.

**Independent execution:**

```bash
./mvnw -pl platform/test-core verify
./mvnw -pl platform/test-reporting -am verify
```

### 3.4 Coordinates (approved — D1)

| Item | Value |
|---|---|
| `groupId` | `io.github.kanhiyaog1997.sdet` |
| Root `artifactId` | `sdet-automation-parent` |
| Layer parent | `platform-parent` |
| Modules | `test-core`, `test-reporting` |
| `version` | `0.1.0-SNAPSHOT` |

---

## 4. Root parent POM

### 4.1 Build properties

| Property | Value | Purpose |
|---|---|---|
| `maven.compiler.release` | `21` | Compiles against the Java 21 API and bytecode — stronger than `source`/`target`; prevents accidental use of newer JDK APIs |
| `project.build.sourceEncoding` | `UTF-8` | Platform-independent compilation and resource handling |
| `project.build.outputTimestamp` | Fixed ISO-8601 timestamp | Reproducible builds: identical sources produce byte-identical jars |
| `argLine` | Empty default | Placeholder JaCoCo populates; Surefire uses `@{argLine}` late binding so the coverage agent is never lost |

### 4.2 Version selection policy (applied uniformly)

Pin **the newest stable release published at least 14 days before selection**. Publication
dates were read from Maven Central directory listings and GitHub release metadata on
2026-11-05 (cutoff 2026-10-22). Re-checked for revision 3 against the 2026-11-06 cutoff
(2026-10-23): every selected version remains eligible (the youngest, Surefire/Failsafe 3.6.1,
was published 2026-10-22), and every excluded version remains too new (published 2026-10-25 or
later). The cutoff is re-applied on the day implementation starts. Pre-releases are never used.
The same rule applies to Maven artifacts and GitHub Actions; Dependabot automates it with a
14-day cooldown (§11.5). **Expected exception:** google-java-format is referenced inside the
Spotless plugin configuration, not declared as a Maven dependency, so Dependabot is not expected
to detect or update it. This has not yet been confirmed by a Dependabot run on GitHub; until it
is, google-java-format is maintained manually under the same rule (R1).

### 4.3 Dependency management (versions only — nothing is added to modules)

| Dependency | Version | Published | Purpose | Used by |
|---|---|---|---|---|
| `org.testng:testng` | 7.12.0 | 2026-01-22 | Test runner — project standard | All tests; `test-reporting` at compile scope (implements listeners) |
| `org.assertj:assertj-bom` | 3.27.7 | 2026-01-24 | Fluent assertions with rich failure messages; BOM aligns AssertJ artifacts. `4.0.0-M1` is a milestone — excluded | Test scope |
| `org.slf4j:slf4j-bom` | 2.0.20 | 2026-09-22 | Logging API; libraries never choose a backend | `test-core`, `test-reporting` |
| `ch.qos.logback:logback-classic` | 1.6.5 | 2026-09-30 | Logging backend, test scope only (1.6.6 published 2026-10-25 — too new) | Test scope |
| `com.fasterxml.jackson:jackson-bom` | 2.22.4 | 2026-10-17 | YAML configuration parsing, JSON pretty-printing and masking. **Jackson 2.x, not 3.x (`tools.jackson` 3.2.4):** RestAssured (Phase 2) integrates with Jackson 2; mixing majors would mean two JSON stacks | `test-core`, `test-reporting` |
| `io.qameta.allure:allure-bom` | **3.0.0** | 2026-09-11 | Aligned Allure Java adapter versions — selection evidence in §5 | `test-reporting` |

### 4.4 Plugin management (every plugin version pinned)

| Plugin | Version | Published | Purpose |
|---|---|---|---|
| `maven-enforcer-plugin` | 3.6.3 | 2026-05-15 | Build-environment rules (§4.5). 3.7.0 (2026-10-26) too new |
| `maven-compiler-plugin` | 3.16.0 | 2026-08-30 | `release 21`, `-parameters`, `-Xlint:all,-processing`, `failOnWarning=true` |
| `maven-surefire-plugin` | 3.6.1 | 2026-10-22 | Unit tests with TestNG; `@{argLine}` for the JaCoCo agent; `trimStackTrace=false`; sets system property `allure.results.directory=${project.build.directory}/allure-results` (§8.6) |
| `maven-failsafe-plugin` | 3.6.1 | 2026-10-22 | Pinned now; activated only by `test-suites` in Phase 2 |
| `maven-resources-plugin` | 3.5.0 | 2026-03-02 | Resource processing |
| `maven-jar-plugin` | 3.5.1 | 2026-07-19 | Jar packaging |
| `maven-clean-plugin` | 3.5.0 | 2025-05-27 | `clean` lifecycle |
| `maven-install-plugin` | 3.2.0 | 2026-09-11 | Local repository install |
| `maven-deploy-plugin` | 3.2.0 | 2026-09-11 | Deploy lifecycle |
| `maven-site-plugin` | 3.22.0 | 2026-05-20 | Site lifecycle (pinned for `requirePluginVersions`) |
| `spotless-maven-plugin` | 3.10.4 | 2026-10-08 | Formatting gate (§6). 3.11.0 (2026-11-02) too new |
| ↳ `google-java-format` | 1.36.1 | 2026-07-30 | Formatter engine — the version Spotless 3.10.4 is built and tested against. **1.37.0 (2026-10-01) is incompatible with Spotless 3.10.4** (formatter crashes inside Spotless; Task 3 finding, see R1). 2.0.0 is a new major (2026-10-30) — too new. **Maintained manually:** referenced inside the Spotless plugin configuration, not declared as a dependency, so Dependabot is not expected to update it (not yet confirmed on GitHub; R1) |
| `maven-checkstyle-plugin` | 3.6.0 | 2024-10-22 | Style-rule gate (§6) — latest release |
| ↳ `com.puppycrawl.tools:checkstyle` | 14.3.0 | 2026-09-27 | Engine override. 14.4.0 (2026-10-31) too new |
| `spotbugs-maven-plugin` | 4.10.5.0 | 2026-10-12 | Static bug-detection gate (§6) |
| ↳ `com.github.spotbugs:spotbugs` | 4.10.5 | 2026-10-12 | Analysis engine |
| `jacoco-maven-plugin` | 0.8.15 | 2026-06-04 | Coverage gate (§6). 0.8.16 (2026-10-26) too new |

`versions-maven-plugin` is not declared: it is used ad hoc from the command line, and
Dependabot covers routine updates; google-java-format is expected to be the exception (R1).

### 4.5 Maven Enforcer rules (bound to `validate`)

| Rule | Setting | Rationale |
|---|---|---|
| `requireJavaVersion` | `[21,22)` (D2) | Java 21 only; local and CI identical |
| `requireMavenVersion` | `[3.9.0,)` | Guards anyone bypassing the wrapper (pinned to 3.10.0) |
| `requirePluginVersions` | Enabled | No plugin may execute at an unpinned version |
| `dependencyConvergence` | Enabled | Conflicting transitive versions fail the build — critical in Phase 4 (Selenium, Playwright, Appium share Netty/Guava) |
| `banDuplicatePomDependencyVersions` | Enabled | Rejects duplicate dependency declarations on Maven 3.9.x, which only warns about them (see below) |
| `banDynamicVersions` | Enabled, with an exact allowlist: `io.github.kanhiyaog1997.sdet:test-core` and `io.github.kanhiyaog1997.sdet:test-reporting` (D10) | Forbids `LATEST`, `RELEASE`, ranges and SNAPSHOTs, except this reactor's own SNAPSHOT modules. Other artifacts in the same group stay banned. Limitation: an allowlisted artifact is skipped whatever its version (R20) |
| `evaluateBeanshell` | No `<version>` on any dependency declared in a child POM (`<dependencies>`, `<dependencyManagement>`, profiles, plugin dependencies); XML comments ignored; the root POM is exempt (D10) | Enforces the `CLAUDE.md` rule that child POMs never declare versions, and keeps ranges off the allowlisted modules (R20). A text match, because Beanshell cannot call the JDK's internal XML parser on Java 21 |

`platform/pom.xml` adds one more Enforcer execution: profile `require-tests-for-production-code`,
active when `src/main/java` exists, runs `requireFilesExist` on `src/test/java` (R18).

**Why `banDuplicatePomDependencyVersions` is kept** (decision R1a, option B). Behaviour was
verified with a deliberately duplicated declaration, in both `<dependencies>` and
`<dependencyManagement>`:

| Maven | Without the rule | With the rule |
|---|---|---|
| 3.10.0 (wrapper) | `[ERROR] '…dependency.(groupId:artifactId:type:classifier)' must be unique` — rejected during model building, before any plugin runs | Same — the model builder rejects it first |
| 3.9.16 (latest 3.9.x) | `[WARNING] … must be unique` — **build succeeds** | `BanDuplicatePomDependencyVersions failed` — build fails |

`requireMavenVersion` stays at `[3.9.0,)`, so builds that bypass the wrapper with a Maven 3.9.x
installation (for example an IDE's bundled Maven) are still allowed; on those versions the
Enforcer rule is what rejects duplicates. The Maven Wrapper remains pinned to **3.10.0**, where
the model builder rejects duplicates on its own.

### 4.6 Lifecycle binding

| Phase | Executes |
|---|---|
| `validate` | Maven Enforcer |
| `compile` | javac — warnings are errors |
| `test` | Surefire + TestNG with the JaCoCo agent |
| `verify` | Spotless check → Checkstyle → SpotBugs → JaCoCo report and check |

`./mvnw test` gives fast inner-loop feedback; `./mvnw verify` is the complete gate and the
exact command CI runs. Gates sit in `verify` so an unformatted file does not block running
tests mid-change, yet nothing merges without passing `verify`.

---

## 5. Allure selection — verified

### 5.1 Candidates (Maven Central, checked 2026-11-05)

| Version | Published | Eligible (≥ 14 days) | Notes |
|---|---|---|---|
| 2.35.5 | 2026-09-04 | Yes | Last 2.x release |
| **3.0.0** | 2026-09-11 | **Yes** | New major: Java 17 baseline, HTTP exchange attachments, TestNG fixes |
| 3.0.1 | 2026-10-26 | No | Release notes: single change — "Bump Jackson to 2.22.4" |
| 3.1.0 | 2026-11-03 | No | — |
| 3.1.1 | 2026-11-04 | No | — |

### 5.2 Compatibility evidence for 3.0.0

| Check | Evidence | Result |
|---|---|---|
| Java 21 | `AllureTestNg.class` bytecode major version **61** (Java 17); allure-java build sets `options.release.set(17)` | Runs on Java 21 ✔ |
| TestNG | `allure-testng/build.gradle.kts` at tag 3.0.0: `testNgVersion = "7.12.0"` (compile-only and test) | Built and tested against **exactly our TestNG 7.12.0** ✔ |
| TestNG dependency | `allure-testng` POM declares **no TestNG dependency** | Consumers must supply TestNG — `test-reporting` declares it at compile scope ✔ |
| Auto-registration | Jar contains `META-INF/services/org.testng.ITestNGListener` | Listener activates without configuration ✔ |
| Transitive deps | `allure-java-commons`, `allure-model`, `slf4j-api 2.0.19` (runtime) | `slf4j-api` resolved to our managed 2.0.20; convergence enforced ✔ |
| Missing fix in 3.0.1? | 3.0.1 changes only Jackson, which we manage centrally at 2.22.4 | Nothing lost by selecting 3.0.0 ✔ |

**Decision:** `allure-bom` **3.0.0**. It is the newest eligible release, is compiled for Java 17
and tested against TestNG 7.12.0, and 2.x offers no advantage for a new codebase. Dependabot
proposes 3.0.1/3.1.x once they age past 14 days. Recorded in ADR-0009.

### 5.3 Allure Maven plugin (used from Phase 2 — recorded now)

| Version | Published | Eligible | Notes |
|---|---|---|---|
| **3.1.0** | 2026-09-16 | **Yes** | Requires JDK 17+; generates **Allure 3** reports by default (runtime 3.17.0) using a plugin-private Node.js — no system Node install required. Fixes Node mirror downloads and relative output directories |

Selected for Phase 2: `allure-maven` **3.1.0**. Phase 2 will pin `reportVersion` explicitly and
verify the Allure 3 report runtime's age under the same policy before enabling it.

---

## 6. Code quality gates — one responsibility per tool (ADR-0006)

| Tool | Configuration | Fails the build on | Remedy |
|---|---|---|---|
| **Spotless 3.10.4 + google-java-format 1.36.1** | Java sources in `src/main` and `src/test`; style `GOOGLE`; Javadoc formatting on; **long-string and text-block reflow on** (§6.3) | Any formatting deviation | `./mvnw spotless:apply` |
| **Checkstyle 14.3.0** | Bundled, unmodified `google_checks.xml` + `build-config/checkstyle-suppressions.xml` (§6.2). Java sources only (`includeResources=false`), test sources included, `violationSeverity=warning` because the Google config reports at warning level | Naming, Javadoc content, imports, braces, switch/fall-through, other coding rules | Fix the code |
| **SpotBugs 4.10.5** | Effort `Max`, threshold `Medium`, main sources. No exclusion file unless a genuine false positive appears — then documented | Null dereferences, exposed internal state, `equals`/`hashCode` mistakes, resource leaks | Fix the code |
| **JaCoCo 0.8.15** | Agent and report on every module; **check only in `platform/pom.xml`** (D3), at bundle level; a platform module with production code but no `src/test/java` fails `validate` (R18) | Platform production code below line 80% / branch 70% | Add meaningful tests |

### 6.1 How the suppression file is wired

The Google configuration contains a built-in optional hook:

```xml
<module name="SuppressionFilter">
  <property name="file" value="${org.checkstyle.google.suppressionfilter.config}"
            default="checkstyle-suppressions.xml"/>
  <property name="optional" value="true"/>
</module>
```

The Maven plugin sets `suppressionsLocation` to
`${maven.multiModuleProjectDirectory}/build-config/checkstyle-suppressions.xml` and
`suppressionsFileExpression` to `org.checkstyle.google.suppressionfilter.config`. The upstream
configuration stays unmodified, so Checkstyle upgrades carry Google-rule improvements
automatically. IntelliJ's Checkstyle plugin is pointed at the same pair.

### 6.2 Suppression validation

**Source of truth.** `google_checks.xml` from `checkstyle-14.3.0.jar` (Maven Central) was
compared with the upstream tag `checkstyle-14.3.0`: byte-identical, 108 module entries.

**Method.** Three probe source files were checked with Checkstyle 14.3.0 before and after
formatting with google-java-format 1.37.0 (plan revisions 2–3, command-line tools). A check that
fires before formatting and never after is **owned by the formatter** (duplicate). A check that
still fires on formatter output is either a genuine code issue or a **conflict**.

**Task 3 acceptance probe — google-java-format 1.36.1 through the real Maven configuration.**
Because 1.37.0 does not run inside Spotless 3.10.4 (R1), the probe was repeated with the pinned
**Spotless 3.10.4 + google-java-format 1.36.1**, using a temporary module under `platform/`
(not in the reactor, deleted afterwards) so the real root and platform POMs and the real
`build-config/checkstyle-suppressions.xml` path were exercised:

| Step | Result |
|---|---|
| Raw code, **empty** suppression file | All **41** suppressed check ids fired individually (82 layout violations), including `LeftCurlyNl`, `OperatorWrapEOL`, every `SeparatorWrap` id and all three `AnnotationLocation` ids |
| Raw code, **real** suppression file | Only the planted content issue (`SummaryJavadoc`) remained — every suppression pattern matches |
| `spotless:apply` | Imports reordered (static first, then lexicographic), unused import removed, longest line 100 |
| Formatted code, **empty** suppression file | Only `SummaryJavadoc` — no suppressed check fires on formatter output; no conflict |
| Content issue fixed by hand, `clean verify` | All gates green: 0 Checkstyle violations, 0 SpotBugs bugs, coverage checks met |

`AnnotationLocationVariables` fires only for a *second* annotation that starts a line at a
column different from its declaration (`allowSamelineMultipleAnnotations=true` makes every other
case legal); the probe covers that case.

**Results.**

- Before formatting: 36 distinct check IDs fired.
- After formatting with long-string reflow on: only `SummaryJavadoc` (content issue planted in
  the probe) and `MultilineCommentLeadingAsteriskPresence` (formatter does not touch `/* */`
  comments) fired.

**Suppressed — layout rules guaranteed by google-java-format (33 modules):**

| Group | Modules | Evidence |
|---|---|---|
| Whitespace | `FileTabCharacter`, `RegexpSingleline` (id `noTrailingWhitespace` only), `WhitespaceAfter`, `WhitespaceAround`, `WhitespaceBeforeEmptyBody`, `ArrayBracketNoWhitespace`, `SingleSpaceSeparator`, `GenericWhitespace`, `NoWhitespaceBefore`, `NoWhitespaceBeforeCaseDefaultColon`, `MethodParamPad`, `ParenPad` | All fired before, none after |
| Line length and wrapping | `LineLength`, `NoLineWrap`, `OperatorWrap`, `SeparatorWrap` | All fired before, none after (`LineLength` requires reflow — §6.3) |
| Braces | `LeftCurly`, `GoogleRightCurly` | Fired before, none after |
| Indentation and vertical spacing | `Indentation`, `CommentsIndentation`, `EmptyLineSeparator`, `TextBlockGoogleStyleFormatting` | Fired before, none after |
| Declaration layout | `ModifierOrder`, `AnnotationLocation`, `OneStatementPerLine` | Fired before, none after (formatter reorders modifiers and splits statements) |
| Imports | `CustomImportOrder` | Fired before, none after (formatter orders imports per Google style) |
| Javadoc layout | `JavadocMissingLeadingAsterisk`, `JavadocLeadingAsteriskAlign`, `JavadocContentLocation`, `JavadocParagraph`, `JavadocTagContinuationIndentation`, `RequireEmptyLineBeforeBlockTagGroup`, `SingleLineJavadoc` | Fired before, none after (formatter rewrites Javadoc layout) |

Suppressions use exact module names (or the specific `id` for `RegexpSingleline`), never
broad patterns. The 33 modules account for 41 of the 108 configuration entries, because
`OperatorWrap`, `SeparatorWrap`, `LeftCurly` and `AnnotationLocation` appear several times with
different `id`s. Every suppressed module fired in the probes before formatting and never after.
Instances not individually triggered by the probes (`LeftCurlyNl`, `OperatorWrapEOL`, the
remaining `SeparatorWrap` ids, `AnnotationLocationTypeAndPackageAndMembers`,
`AnnotationLocationVariables`) are covered explicitly by the task 3 acceptance test (§6.4).

**Kept active — not owned by the formatter (46 checks and 9 filters; the remaining 2 of the
67 kept entries are the `Checker` and `TreeWalker` containers):**

| Category | Modules |
|---|---|
| Naming | `PackageName`, `TypeName`, `GoogleNonConstantFieldName`, `ParameterName`, `LambdaParameterName`, `CatchParameterName`, `RecordComponentName`, `LocalVariableName`, `LocalFinalVariableName`, `PatternVariableName`, `ClassTypeParameterName`, `RecordTypeParameterName`, `MethodTypeParameterName`, `InterfaceTypeParameterName`, `GoogleMethodName`, `AbbreviationAsWordInName` |
| Javadoc content | `JavadocMethod`, `MissingJavadocMethod`, `MissingJavadocType`, `SummaryJavadoc`, `NonEmptyAtclauseDescription`, `AtclauseOrder`, `InvalidJavadocPosition` |
| Coding rules | `NeedBraces`, `MissingSwitchDefault`, `FallThrough`, `EmptyCatchBlock`, `NoFinalizer`, `MultipleVariableDeclarations`, `VariableDeclarationUsageDistance`, `OverloadMethodsDeclarationOrder`, `ConstructorsDeclarationGrouping`, `MissingOverrideOnRecordAccessor`, `UpperEll`, `ArrayTypeStyle` |
| Imports and structure | `AvoidStarImport`, `AvoidModuleImport`, `OneTopLevelClass`, `OuterTypeFilename`, `PackageDeclaration`, `ModuleDirectiveOrder`, `MatchXpath` (compact source files) |
| Text and comments | `IllegalTokenText`, `AvoidEscapedUnicodeCharacters`, `TodoComment` (TODO format, box comments), `MultilineCommentLeadingAsteriskPresence` |
| Filters (infrastructure) | `SuppressWarningsFilter`, `SuppressionFilter`, `SuppressWithNearbyTextFilter`, `SuppressWithPlainTextCommentFilter`, `SuppressionXpathSingleFilter`, `SuppressionXpathFilter`, `SuppressWarningsHolder`, `SuppressionCommentFilter`, `SuppressWithNearbyCommentFilter` |

`MultilineCommentLeadingAsteriskPresence` is a layout rule but is **kept**: the formatter does
not own it, so it is not a duplicate; leaving it active keeps block comments consistent.
ADR-0007 records this as a clarification of ADR-0006.

**Test code.** `MissingJavadocMethod` and `JavadocMethod` allow `@Test` and `@Override`, so
test methods do not need Javadoc. `AbbreviationAsWordInName` uses
`allowedAbbreviationLength=0`: names such as `ApiClient` are required instead of `APIClient`.

### 6.3 Spotless must reflow long strings

| Spotless setting | Checkstyle result on formatter output |
|---|---|
| Default (`reflowLongStrings` off) | `LineLength`, `TextBlockGoogleStyleFormatting` and `Indentation` **still fire** — the formatter skips long-string *and* text-block reformatting, producing code Checkstyle rejects but Spotless accepts |
| `reflowLongStrings` **on** | No formatting check fires |

Spotless is therefore configured with `reflowLongStrings=true` and `formatJavadoc=true`, so the
formatter is the only authority and never contradicts Checkstyle.

### 6.4 Acceptance test in task 3

The probe is repeated **through the real Spotless and Checkstyle Maven configuration**:
format with `spotless:apply`, then `checkstyle:check` must report no formatting violations.
The probe is extended to trigger every suppressed `id` individually (including those listed in
§6.2 as not yet triggered), each suppression is re-checked by temporarily removing it, and any
discrepancy is resolved before the gates are accepted.

---

## 7. Module: `platform/test-core`

### 7.1 Purpose

Typed, layered test configuration and safe secret handling. Every framework from Phase 2 onward
reads its settings — base URLs, timeouts, browser choice, credentials — through this module.

### 7.2 Dependencies

| Dependency | Scope | Purpose |
|---|---|---|
| `jackson-databind`, `jackson-dataformat-yaml` | compile | Parse YAML configuration. Maintained; the Owner library is unmaintained |
| `slf4j-api` | compile | Log which configuration sources were loaded — never secret values |
| `testng`, `assertj-core`, `logback-classic` | test | Unit tests and test-time logging |

### 7.3 Configuration model

```
lowest precedence                                                          highest precedence
classpath:config/default.yml  <  classpath:config/<env>.yml  <  environment variables  <  -D system properties
```

This precedence applies to ordinary keys. Keys in the `secret.` namespace come **only** from
`SECRET_*` environment variables; they are rejected in YAML files and as system properties (D7,
§7.5).

- **Environment selection:** `-Dtest.env=staging` or `TEST_ENV=staging`; default `local`. An
  explicitly selected environment whose file is missing fails fast.
- **Single key-mapping rule:** YAML `api.base-url` ⇔ `-Dapi.base-url` ⇔ `API_BASE_URL`
  (dots and dashes become underscores, upper-cased).

### 7.4 Classes

| Class | Responsibility |
|---|---|
| `TestConfig` | Immutable, thread-safe view: `require`, `find → Optional`, `requireInt`, `requireBoolean`, `requireDuration`, `requireUri`, `requireSecret`. Missing keys fail with a message listing every source consulted. `nonSecretEntries()` for report environment info; `secretValues()` for masking |
| `ConfigLoader` | Merges the four layers; rejects `secret.*` keys in YAML and as system properties. Environment variables and system properties are injected as maps, so tests never mutate the process environment and need no mocking |
| `Secret` | Wraps sensitive values: `toString()` returns `****`; the raw value only via explicit `reveal()` |
| `ConfigException` | Single unchecked exception type with actionable messages |

### 7.5 "No hardcoded credentials" — enforced in code

Any `secret.*` key found in a YAML file causes loading to fail:

> `secret.admin-password` must be supplied via environment variable `SECRET_ADMIN_PASSWORD`.

**Changed in Task 5 (D7):** secrets come only from `SECRET_*` environment variables. A
`-Dsecret.*` system property makes loading fail with a message naming the property and the
environment variable to use, never the value. See R21 for what the rejection cannot prevent.

### 7.6 `TestConfig.current()`

A lazily loaded, cached instance, existing only because TestNG instantiates listeners via
no-argument constructors. All other code receives a `TestConfig` explicitly (ADR-0008).

### 7.7 Unit tests

- Layer precedence across all four sources.
- Environment-variable and system-property key mapping, including dashes and nesting.
- Missing required key: message names all consulted sources.
- Explicit environment with no matching file: fails.
- Type conversion for `int`, `boolean`, `Duration`, `URI` — valid and invalid input.
- `secret.*` keys in YAML rejected; `secret.*` system properties rejected without echoing their
  values, even when the environment variable is also set; only the `secret.` namespace is
  affected; secrets from environment accepted.
- `Secret.toString()` masked; `nonSecretEntries()` never contains secrets.
- Immutability of the configuration view.
- Concurrent `current()` calls return the same instance.

Implemented in Task 4 (30 tests in `TestConfigTest` after the Task 5 change).

### 7.8 Security review findings (Task 4)

| # | Finding | Resolution |
|---|---|---|
| S1 | An invalid-YAML error could echo file content, including a secret, through the parser's message or cause | **Fixed in Task 4:** the message reports only the resource and the line/column; no cause is attached; a missing location is handled. Regression-tested |
| S2 | Secret protection depends on naming: a credential under a non-`secret.*` key (for example `db.password`) or an environment variable such as `AWS_SECRET_ACCESS_KEY` is plain configuration and is not in `secretValues()` | By design. Documented in the `test-reporting` README (security limitations), the `test-core` README and ADR-0008 (Task 7a) |
| S3 | `Secret.reveal()` inside an assertion prints the value when the assertion fails | Usage guidance: never use `reveal()` for logging, reporting or assertion messages. Documented in the `test-reporting` README, the `test-core` README and ADR-0008 (Task 7a) |
| S4 | Very short secret values would over-mask ordinary text | **Implemented in Task 5:** value-based masking ignores secrets shorter than four characters; key-based masking still applies (§8.3) |

---

## 8. Module: `platform/test-reporting`

### 8.1 Purpose

One consistent Allure integration for every suite; mandatory masking of sensitive data in all
attachments (README §10); test-aware logging that stays readable under parallel execution.

### 8.2 Dependencies

| Dependency | Scope | Purpose |
|---|---|---|
| `test-core` | compile | Environment information and known secret values for masking |
| `allure-testng` | compile | Allure's TestNG adapter; auto-registers via `META-INF/services` (verified §5.2) |
| `allure-java-commons` | compile | Allure API used directly: attachments, lifecycle, results writer, results-directory resolution |
| `testng` | compile | Implements TestNG listener interfaces; also required because `allure-testng` declares no TestNG dependency (§5.2) |
| `jackson-databind` | compile | Pretty-printing of JSON attachments |
| `slf4j-api` | compile | Logging and MDC |
| `assertj-core`, `logback-classic` | test | Tests |

### 8.3 Classes

| Class | Responsibility |
|---|---|
| `SensitiveDataMasker` | Masks by **exact value** every secret from `TestConfig.secretValues()` of at least four characters (S4), merging overlapping matches; then by **key name**, case-insensitive (keys containing `authorization`, `cookie`, `password`, `passwd`, `token`, `secret`, `api-key`/`api_key`/`apikey`, `credential`) in header lines, JSON members and `key=value` text, plus `Bearer` tokens anywhere. Text-based rules, no I/O |
| `ReportAttachments` | `text(...)`, `json(...)` (pretty-printed; invalid JSON attached as masked text), `png(...)`. Text and names pass through the masker first — unmasked text cannot be attached through this API |
| `AllureEnvironmentListener` | On execution start, writes `environment.properties` (environment name, non-secret configuration, Java version, OS) to the Allure results directory |
| `TestLoggingListener` | Puts the test name in the SLF4J MDC under `testName`; logs start, pass, fail and skip with durations; failure messages masked, then truncated |
| `AllureResultMasker` | Allure `TestLifecycleListener` and `ContainerLifecycleListener` (D8): masks every text field of results and containers, recursively through steps and fixtures, immediately before they are written; fails closed with `<masking failed>`; never changes identifiers, `historyId`, relationships or attachment sources |
| `MaskingResultsWriter` | Wraps Allure's `FileSystemResultsWriter` (D9): masks run-level errors (`*-globals.json`) and global attachment names before writing; passes everything else through |
| `AllureLifecycleInstaller` | TestNG listener (D9) that installs an Allure lifecycle writing through `MaskingResultsWriter`, once per JVM, before `AllureTestNg` is constructed; throws if `AllureTestNg` is registered first |
| `META-INF/services/org.testng.ITestNGListener` | Registers `AllureLifecycleInstaller` (first), `AllureEnvironmentListener` and `TestLoggingListener` |
| `META-INF/services/io.qameta.allure.listener.{TestLifecycleListener,ContainerLifecycleListener}` | Register `AllureResultMasker` with Allure |

### 8.4 Architectural decisions

- **No AspectJ agent.** `@Step`/`@Attachment` annotations need a load-time weaver; the
  `Allure.step(...)` lambda API and `ReportAttachments` meet the same needs without it.
  Revisited in Phase 2 only with clear readability benefit.
- **Results in `target/allure-results`, configured explicitly** — see §8.6 for why the default
  is wrong and how it is verified.
- **Report generation and Pages publishing in Phase 2** with `allure-maven` 3.1.0 (§5.3).
- **Masking happens at write time** (D8, D9): every Allure output is masked in the last
  callback or writer call before it reaches disk, so a value that slips past earlier layers is
  still caught. Remaining limitations (direct `Allure.attachment` content, data-provider
  parameters and `historyId`, Surefire XML) are listed in the module README.

### 8.5 Tests

- **Masker:** every sensitive key, case-insensitivity, nested JSON and arrays, malformed JSON
  falling back to text masking, query strings, exact-value secrets, null/empty input,
  non-sensitive data untouched.
- **Environment writer:** temporary directory, no secrets, deterministic key order.
- **Wiring test:** runs a sample class through the real TestNG engine inside a unit test and
  asserts that:
  - the configured `allure.results.directory` resolves to exactly
    `<module>/target/allure-results` (compared as absolute paths);
  - result files for the sample class and `environment.properties` exist **in that exact
    directory**;
  - no `<module>/allure-results` directory (Allure's fallback location) was created;
  - both listeners executed — proving ServiceLoader auto-registration end to end.

  The sample class is named so Surefire never collects it as a real test. The wiring test moves
  the files it created to `target/allure-sample-results/wiring/` (changed in Task 5 from
  deleting them), so module reports contain real tests only while the sample output stays
  available for inspection and secret scans.

**Implemented in Task 5** (87 tests): `SensitiveDataMaskerTest` (21), `ReportAttachmentsTest`
(5), `AllureEnvironmentListenerTest` (9), `TestLoggingListenerTest` (10), `ReportingWiringTest`
(11), `AllureResultMaskerTest` (6), `AllureResultPersistenceTest` (9, recording results writer),
`AllureResultMaskingEndToEndTest` (4, real files; output moved to
`target/allure-sample-results/end-to-end/`), `MaskingResultsWriterTest` (6) and
`AllureLifecycleInstallerTest` (6). The leak samples cover failures, skips, steps, links,
parameters, a failing configuration method and a failing data provider.

### 8.6 Allure results directory — verified

**Evidence from Allure 3.0.0 source:**

| Source | Finding |
|---|---|
| `AllureLifecycle.getDefaultWriter()` | `properties.getProperty("allure.results.directory", "allure-results")` → `new FileSystemResultsWriter(Paths.get(path))` |
| `PropertiesUtils.loadAllureProperties()` | Loads `allure.properties` from the classpath, then `properties.putAll(System.getProperties())` — **system properties take precedence** |
| `FileSystemResultsWriter` | Creates the directory if missing; writes each file to a `.allure-write-*.tmp` file and moves it into place (atomic move where supported) |
| 3.0.0 release notes | "Leave Allure results cleanup to build tools" — Allure never deletes old results |

**Consequence.** The default `allure-results` is relative to the JVM working directory.
Surefire runs tests from the module base directory, so without configuration results land in
`<module>/allure-results` — **outside `target/`, not removed by `./mvnw clean`**, and
accumulating across runs. (`.gitignore` already ignores `allure-results/` everywhere, so they
would never be committed, but they would pollute reports.)

**Configuration.** The root POM sets, in Surefire's `systemPropertyVariables`:

```xml
<allure.results.directory>${project.build.directory}/allure-results</allure.results.directory>
```

`${project.build.directory}` is absolute, so the location no longer depends on the working
directory. `test-reporting` deliberately ships **no** `allure.properties`: a file in a
library's `src/main/resources` would land on every consuming suite's classpath and impose
its value on them. The same Surefire setting is reused by Failsafe for suites in Phase 2.

**Stale results.** Because Allure never cleans up, `./mvnw verify` run repeatedly without
`clean` accumulates results from earlier runs in `target/allure-results`. CI always starts
from a fresh checkout, so CI reports are never stale. Locally, `./mvnw clean verify` is the
documented command whenever a report will be generated (module READMEs, Phase 2 report
tooling).

**Verification in task 5:**

| Step | Expected |
|---|---|
| `./mvnw -pl platform/test-reporting -am verify` | Results and `environment.properties` exist in `platform/test-reporting/target/allure-results/`; no `platform/test-reporting/allure-results/` exists |
| Create a marker file `stale-result.json` in `target/allure-results/`, run `./mvnw -pl platform/test-reporting -am verify` | Marker **still present** — confirms Allure does not clean (documents the accumulation behaviour) |
| Run `./mvnw -pl platform/test-reporting -am clean verify` | Marker **gone**; only results from this run present |
| Run `./mvnw -pl platform/test-reporting clean` | `target/allure-results/` no longer exists |

**Status.** The first row is asserted on every build by `ReportingWiringTest` (exact directory,
no fallback directory). Evidence for the three marker and `clean` rows was not recorded in the
Task 5 report; it was recorded in Task 8 step 1 on 2026-11-07 (O6): the marker was still present
after `verify`, was gone after `clean verify` with only that run's results left, and `clean`
removed `target/allure-results/` (all four commands exit 0).

---

## 9. Logging strategy

| Concern | Decision |
|---|---|
| API | SLF4J only in library code |
| Backend | Logback at **test scope** in platform modules; suites choose it at runtime scope from Phase 2 |
| Configuration | `src/test/resources/logback-test.xml` per module; libraries ship no logging configuration |
| Correlation | Test name in MDC via `TestLoggingListener` |
| Secrets | Never logged; configuration logging prints keys and sources only |

---

## 10. Build validation

These checks were exercised during Tasks 2–6 and reported with each task. Task 8 step 1
(2026-11-07) repeated the complete set on the final branch (`3da20fb`): every positive check and
the exact CI command passed, and every negative control failed at its intended gate. The
negative controls ran in disposable `git archive` copies instead of as reverted edits in the
repository; the faults and expected failures are those in §10.2.

### 10.1 Positive validation

| Command | Proves |
|---|---|
| `./mvnw -B verify` | Enforcer, compilation, tests and all four gates pass |
| `./mvnw -pl platform/test-core verify` | Module builds and tests in isolation |
| `./mvnw -pl platform/test-reporting -am verify` | Dependent module builds with only what it needs |
| `./mvnw dependency:tree` | No unexpected dependencies; Logback only at test scope; SLF4J converged |
| `./mvnw -B verify` (twice) | Repeatable — no order-dependent or stateful tests |
| Allure directory checks (§8.6) | Results only in `target/allure-results`; stale results removed by `clean` |
| `git check-attr` on `*.har` after the `.gitattributes` change (§13) | `text: set`, `diff: set`, `eol: lf`, `binary: unspecified` |

### 10.2 Negative validation

Faults are introduced temporarily, reverted, and never committed.

| Deliberate fault | Expected failure |
|---|---|
| Mis-formatted Java file | `spotless:check` |
| Non-conforming method name (`Get_value`) | Checkstyle `GoogleMethodName` |
| Method exposing an internal mutable list | SpotBugs `EI_EXPOSE_REP` |
| Untested class dropping coverage below threshold | JaCoCo check |
| Unpinned plugin version | Enforcer `requirePluginVersions` |
| `secret.*` key in a YAML file | `test-core` test / runtime `ConfigException` |
| Formatter output checked by Checkstyle | Must **pass** — no formatting conflicts (§6.4) |
| Surefire `allure.results.directory` setting temporarily removed | Wiring test **fails**: results appear in `<module>/allure-results`, not the exact expected directory |

---

## 11. Continuous integration

### 11.1 Action versions — verified 2026-11-05

| Action | Selected | Published | Full commit SHA (verified via GitHub API) | Runtime | Not selected |
|---|---|---|---|---|---|
| `actions/checkout` | **v7.0.1** | 2026-07-20 | `3d3c42e5aac5ba805825da76410c181273ba90b1` | node24 | — |
| `actions/setup-java` | **v6.1.0** | 2026-10-14 | `b843ebf9b913146b38da2eaa7aeddf76074ae1ad` | node24 | v6.2.0 (2026-11-04) too new |
| `actions/upload-artifact` | **v7.0.2** | 2026-10-07 | `cf430e030ddbb5b0abf93d22962f4752f3646cd9` | node24 | — |

Each tag was resolved with `git ls-remote` against the action's own repository and the SHA
confirmed as a commit in that repository via the GitHub API. Every input used by the workflow
was confirmed against the action's `action.yml` at that SHA.

**Re-verified 2026-11-06 (Task 6):** all three tags still resolve to the SHAs above, all three
actions run on node24, and every input used exists at the pinned SHA. `setup-java` v6.2.0
(2026-11-04) is still younger than 14 days, so v6.1.0 stays.

### 11.2 `.github/workflows/ci.yml` (implemented in Task 6)

The committed file (Task 6, `8064973`) is byte-for-byte identical to this block (checked by
script in Task 6 and again in Task 7).

```yaml
name: CI

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]
  workflow_dispatch:

permissions:
  contents: read

concurrency:
  group: ci-${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: ${{ github.event_name == 'pull_request' }}

jobs:
  build:
    name: Build and verify
    runs-on: ubuntu-24.04
    timeout-minutes: 15
    steps:
      - name: Check out repository
        uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
        with:
          persist-credentials: false

      - name: Set up JDK 21
        uses: actions/setup-java@b843ebf9b913146b38da2eaa7aeddf76074ae1ad # v6.1.0
        with:
          distribution: temurin
          java-version: "21"
          cache: maven

      - name: Build and verify
        id: verify
        run: ./mvnw --batch-mode --no-transfer-progress --fail-at-end verify

      - name: Generate coverage report after a failed build
        if: failure() && steps.verify.outcome == 'failure'
        run: ./mvnw --batch-mode --no-transfer-progress --offline jacoco:report

      - name: Upload test, coverage and analysis reports
        if: ${{ !cancelled() }}
        uses: actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9 # v7.0.2
        with:
          name: build-reports
          path: |
            **/target/surefire-reports/
            **/target/site/jacoco/
            **/target/allure-results/
            **/target/checkstyle-result.xml
            **/target/spotbugsXml.xml
          if-no-files-found: warn
          retention-days: 14
```

### 11.3 Design rationale

| Choice | Rationale |
|---|---|
| Triggers: PRs and pushes to `main`, manual dispatch | Validate proposals; confirm `main` stays green after merge. `branches: [main]` on `pull_request` filters on the PR's target branch |
| `pull_request`, not `pull_request_target` | A PR from a fork runs with a read-only token and no repository secrets, even if it changes this workflow |
| `permissions: contents: read` | Least privilege — a compromised step cannot push |
| `persist-credentials: false` | The job never pushes, so the token is not left in `.git/config` |
| Concurrency cancels only PR runs | Each PR has its own group (`refs/pull/<n>/merge`): a newer push cancels that PR's running build, because only the latest commit matters for review. On `main` (`refs/heads/main`, shared by pushes and manual runs) `cancel-in-progress` is false, so a **running** build is never cancelled by a newer push. GitHub keeps at most **one running and one pending** run per concurrency group, and a newer push replaces an older pending run. In a rapid sequence of pushes to `main`, a middle commit may therefore get no run of its own; the latest commit is always validated. Not every commit pushed to `main` is guaranteed an individual run |
| Actions pinned by full SHA with version comment | Tags are mutable; SHAs are not. Dependabot updates both |
| `ubuntu-24.04` | Runner image changes cannot silently change the build |
| `--fail-at-end` | One failing module does not stop independent modules from testing and reporting |
| Coverage report on failure | When tests fail, Maven stops before `verify`, so JaCoCo's report goal never runs. The follow-up step builds the report from the execution data already recorded — coverage stays available exactly when it is most needed |
| `id: verify` and `failure() && steps.verify.outcome == 'failure'` | The coverage step runs **only when the Maven build itself failed**. If checkout or JDK setup fails, the Maven step is skipped (`outcome == 'skipped'`), so running Maven again would be pointless and would add a misleading second error. `failure()` is required: without a status function GitHub implicitly adds `success()`, and the step could never run after a failure |
| `outcome` rather than `conclusion` | `outcome` is the step's result before `continue-on-error`; the two are identical here, and `outcome` stays correct if `continue-on-error` is ever added |
| `if: ${{ !cancelled() }}` on upload | Uploads after success **and** failure; skips cancelled runs whose output is meaningless. A run that reaches `timeout-minutes: 15` is expected to upload nothing (not exercised) |
| Uploaded reports | One artifact, `build-reports`: `**/target/surefire-reports/`, `**/target/site/jacoco/`, `**/target/allure-results/`, `**/target/checkstyle-result.xml`, `**/target/spotbugsXml.xml`. After a test failure, the failing module has no Checkstyle or SpotBugs XML (they run in `verify`, after `test`); modules that finished keep theirs. `target/allure-sample-results/` is deliberately not uploaded |
| `if-no-files-found: warn` | A compile failure (no reports) is visible in the log without masking the real failure |
| `retention-days: 14` | Long enough to investigate; limits storage |
| Secrets only through `env:` | **Mandatory for every workflow (D7, R21).** When a job needs a secret, it is mapped in the job's or step's `env:` block as `SECRET_*`, never passed on the Maven command line and never as `-Dsecret.*`: system properties appear in process listings and are copied into Surefire and Failsafe XML reports, which this workflow uploads. The Phase 1 workflow needs no secrets |

**After the first green run:** add the job's check as a required status check for `main`.
**Done (2026-11-06):** the repository ruleset `Protect main` (ID `26725062`, active, target: the
default branch) requires the status check **`Build and verify`** from GitHub Actions. That is the
exact check name GitHub reports (the job's `name:`); GitHub displays it in pull requests as
"CI / Build and verify" (workflow / job). The ruleset also requires a pull request (0 approvals),
requires branches to be up to date before merging, blocks force pushes and restricts deletion;
linear history is not required. Its bypass list is empty, as confirmed by the repository owner in
the GitHub UI (not readable through the unauthenticated API).

### 11.4 Workflow validation

| Check | Tool | Result on the planned workflow |
|---|---|---|
| Syntax, keys, expressions, runner labels (revision 3 workflow, with `id: verify`) | actionlint **1.7.12** (2026-03-30; SHA-256 verified against the release checksums) | **No problems found** (exit 0) |
| actionlint detects real errors | Negative controls: broken `${{ }}` expression; misspelled job key | Both reported (exit 1) |
| Step-id reference is type-checked | Negative control: `steps.verfy.outcome` | Reported: `property "verfy" is not defined in object type {verify: …}` |
| YAML parses | Ruby standard library YAML | 1 job, 5 steps |
| Action inputs | Manual check against each `action.yml` at the pinned SHA | All inputs exist |

**Known actionlint limitations (verified by negative controls):**

| Not detected | Mitigation |
|---|---|
| Invalid outcome literal (`outcome == 'failed'` passed with exit 0) | Exact literal `'failure'` checked in review; proven by the behavioural run below |
| `id: verify` placed on the wrong step (only existence of the id is checked) | Review; proven by the behavioural run below |
| Inputs of SHA-pinned actions | Manual `action.yml` check |

During task 6 actionlint runs from the session scratch directory; nothing is installed
system-wide.

**Task 6 results** (2026-11-06; tools in the session scratch directory only):

| Check | Result |
|---|---|
| actionlint 1.7.12 (SHA-256 verified) on the implemented file | No problems (exit 0). Run without shellcheck: downloading an unverified shellcheck binary was not permitted; the two `run:` lines are single `./mvnw` invocations |
| SchemaStore `github-workflow.json` / `dependabot-2.0.json` (`jsonschema` 4.23.0) | Both valid; the schema defines `cooldown.default-days` (1–90) |
| actionlint negative controls | Detected (exit 1): broken expression, `runs_on`, `steps.verfy.outcome`, permission `content`, runner `ubuntu-2404`, a step dedented out of `steps:`. Not detected: `outcome == 'failed'` (R14) |
| Dependabot schema negative controls | Detected: `default-days: 100`, `default_days`, interval `fortnightly`, ecosystem `github-action`, a group dedented to the level of `groups:`. **Not detected:** a group without `patterns` (review) |
| Exact CI command on a clean `git archive HEAD` export | Exit 0; every upload path matched files (Surefire 22, JaCoCo 74, Allure 250, Checkstyle 3, SpotBugs 2) |
| Failure path on that export, with a deliberate failing test (export only) | `verify` exit 1; `--fail-at-end` let `test-core` finish; the failing module had no JaCoCo HTML until `--offline jacoco:report` (exit 0) produced it — R13 confirmed locally |
| Full `./mvnw -B -ntp clean verify` in the repository | Exit 0; 117 tests; gates green |

The `if:` conditions themselves are evaluated only by GitHub. They were proven on GitHub in
Task 8 for the green and test-failure scenarios; the setup-failure scenario was not executed
(below).

**Behavioural validation of the conditional.** Docker and `act` are not available locally, and
`if:` conditions are evaluated only by the GitHub runner, so behaviour is proven on GitHub:

| Scenario | `steps.verify.outcome` | `failure()` | Coverage step | Upload step | Where it is exercised |
|---|---|---|---|---|---|
| All green | `success` | false | **skipped** | runs | Phase 1 PR run (task 8) |
| Test failure | `failure` | true | **runs**; JaCoCo HTML present in the artifact | runs | Temporary validation PR (task 8, approval required) |
| JDK setup fails (e.g. invalid `java-version`) | `skipped` | true | **skipped** | runs; warns that no files were found | Temporary validation PR (task 8, approval required) |
| Run cancelled | — | — | skipped | **skipped** (`!cancelled()`) | Not forced; standard GitHub semantics |

The temporary validation PR uses a throwaway branch (`ci-validation/failure-paths`) with one
commit per scenario. It is opened as a draft, never merged, then closed and the branch deleted.
Pushing it and opening it require your explicit approval at that time; the run links are
reported as evidence.

**Task 8 results** (2026-11-06, GitHub; the planned table above is kept unchanged):

| Scenario | Run | Observed | Status |
|---|---|---|---|
| All green | PR #2: [42279950007](https://github.com/kanhiyaOG1997/sdet-automation/actions/runs/42279950007); push to `main`: [42279970970](https://github.com/kanhiyaOG1997/sdet-automation/actions/runs/42279970970); PR #3 after the probe was removed (`f97c3e9`): [42296537859](https://github.com/kanhiyaOG1997/sdet-automation/actions/runs/42296537859) | Build and verify success; coverage step **skipped**; upload ran; `build-reports` artifact with 14-day retention | **Completed** |
| Test failure | PR #3, probe commit `b8383c8`: [42295149608](https://github.com/kanhiyaOG1997/sdet-automation/actions/runs/42295149608) | A single deliberate failing test (`CiFailurePathProbeTest`, `test-core`, validated locally first: 1 failure, 30 other tests passing). Build and verify **failure**; coverage step **ran** (success); upload ran (artifact 82,865 bytes); check `Build and verify` concluded failure | **Completed.** Not independently verified: that the failure artifact contains JaCoCo HTML (downloading artifacts requires authentication) |
| JDK setup fails | — | Not executed | **Accepted residual risk** (R14, R22) |
| Run cancelled | — | Not forced | Standard GitHub semantics, as planned |

**Merge blocking** (PR #3): while the required check `Build and verify` was failing, GitHub
blocked the merge. This is **confirmed by the repository owner in the GitHub UI**; it was not
independently verified through the API (the unauthenticated API reported
`mergeable_state: unstable`, so the API alone does not show the block).

**Execution deviations from the plan above:** PR #3 was opened as a regular pull request, not a
draft, because a draft is unmergeable for its own reason and would hide the check-based block.
It had two commits, the probe (`b8383c8`) and its removal (`f97c3e9`), after which the branch
had no file changes against `main`. PR #3 was closed without merging; the remote branch was
deleted, and the local branch is kept as a reference.

### 11.5 `.github/dependabot.yml`

| Setting | Value | Rationale |
|---|---|---|
| Ecosystems | `maven`, `github-actions` | Dependencies, plugins and workflow actions (SHA pins included) |
| Schedule | Weekly | Steady, reviewable cadence |
| Groups | Build plugins, test libraries, GitHub Actions | Fewer, coherent PRs |
| Cooldown | 14 days | Automates the policy in §4.2 |

**Implemented in Task 6** (validated against the Dependabot schema, see §11.4):

| Ecosystem | Directory | Schedule | Cooldown | Groups |
|---|---|---|---|---|
| `maven` | `/` | weekly | `default-days: 14` | `build-plugins`: `org.apache.maven.plugins:*`, `com.diffplug.spotless:*`, `com.puppycrawl.tools:*`, `com.github.spotbugs:*`, `org.jacoco:*`; `test-libraries`: `org.testng:*`, `org.assertj:*`, `io.qameta.allure:*` |
| `github-actions` | `/` | weekly | `default-days: 14` | `github-actions`: `*` |

**Coverage limits** (to be confirmed by the first Dependabot run on GitHub, Task 8 or later):

| Item | Expected behaviour |
|---|---|
| google-java-format 1.36.1 | **Not expected to be updated:** its version is referenced inside the Spotless plugin's `<configuration>`, not declared as a Maven dependency or plugin dependency. Not yet confirmed: the first Dependabot run must explicitly check whether a google-java-format update is proposed. Maintained manually until verified (R1). **First run (2026-11-06):** both updates succeeded (Maven [42279981949](https://github.com/kanhiyaOG1997/sdet-automation/actions/runs/42279981949), GitHub Actions [42279982448](https://github.com/kanhiyaOG1997/sdet-automation/actions/runs/42279982448)) and opened no PRs. No google-java-format PR was proposed although 1.37.0 is past the cooldown, which supports this expectation; it is not conclusively confirmed (the Dependabot logs require authentication), so manual maintenance continues |
| Checkstyle and SpotBugs engines | Declared as plugin dependencies; expected to be covered |
| Jackson, SLF4J and Logback BOMs/artifacts | Not grouped: individual PRs |
| `io.github.kanhiyaog1997.sdet:*` | Not published, so no update PRs are expected |
| Maven Wrapper (`distributionUrl` 3.10.0) | Outside the approved scope; whether the `maven` ecosystem covers it is unverified |

---

## 12. Documentation deliverables

Status on 2026-11-07. The deliverables that were outside the Task 7 scope were delivered in
Task 7a (O4, §18), committed as `c83768f` after the Task 7 and Task 6 commits (§15).

| File | Content | Status |
|---|---|---|
| `docs/adr/0007-build-and-quality-gates.md` | Layer parents, enforcer rules (including the D10 allowlist and child-POM version rule), version-age policy, gate binding, Checkstyle suppression method and evidence, `reflowLongStrings`, the `MultilineCommentLeadingAsteriskPresence` clarification, manual google-java-format maintenance (R1) | **Done** (Task 7a) |
| `docs/adr/0008-test-configuration-and-secrets.md` | Layer precedence, key mapping, environment-only secrets (D7), `current()` vs explicit injection, S2–S4 (§7.8) | **Done** (Task 7a) |
| `docs/adr/0009-reporting-allure-and-masking.md` | Allure 3.0.0 selection and evidence, no AspectJ, results directory and its verification, mask-by-default attachments, write-time masking (D8, D9), `allure-maven` 3.1.0 for Phase 2 | **Done** (Task 7a) |
| `platform/test-core/README.md` | Purpose, configuration keys, usage, how to run; S2 and S3 guidance; the R18 limitation | **Done** (Task 7a; required by `CLAUDE.md`: every major module has a README) |
| `platform/test-reporting/README.md` | Purpose, listener behaviour, attachments, masking rules, results location, `clean verify` guidance, how to run | **Done in Task 5** (includes secret-supply rules, write-time masking and security limitations) |
| `platform/README.md` | Module status updated | **Done** (Task 7a) |
| `README.md` | Repository structure gains `build-config/` and `docs/plans/`; real build commands; ADR table gains 0007–0009 | **Done** (Task 7a) |
| `docs/adr/README.md` | Index gains 0007–0009 | **Done** (Task 7a) |
| `CLAUDE.md` | Real build and quality commands | **Done** (Task 7a) |
| `.gitattributes` | Approved HAR change (D5, §13.4) | **Done** (Task 7a; see the §13.4 status note) |
| `docs/plans/phase-1.md` | This plan, relocated and reconciled | **Done** (Task 7, `9868919`; old-path references updated in Task 7b, `3306dc1`) |

---

## 13. `.gitattributes` — `*.har` analysis

### 13.1 Current rules (`origin/main`)

```text
12: # JMeter plans are XML; diff them as text.
13: *.jtl text eol=lf diff
14: *.har text eol=lf diff
25: *.har binary
```

### 13.2 Effective behaviour (`git check-attr`)

| File | `text` | `eol` | `diff` | `merge` | `binary` |
|---|---|---|---|---|---|
| `sample.har` | **unset** | lf | **unset** | **unset** | **set** |
| `sample.jtl` | set | lf | set | unspecified | unspecified |
| `plan.jmx` | auto | lf | unspecified | unspecified | unspecified |

### 13.3 Technical findings

1. **Line 14 has no effect.** When several lines match a path, the later one wins. Line 25's
   `binary` macro expands to `-diff -merge -text`, overriding `text` and `diff` from line 14.
   The remaining `eol=lf` is also inert: Git applies `eol` only when `text` is set or
   unspecified, and here it is unset. The two lines contradict each other, and the effective
   behaviour (binary) is the opposite of what line 14 states.
2. **The underlying question is which treatment HAR files should get.** HAR files are UTF-8
   JSON. The repository's security guidelines require HAR files to be sanitized before they
   are committed (README §11). Text diffs let a reviewer *see* in a pull request that cookies,
   tokens and personal data were removed; binary treatment hides the content from review.
   HAR fixtures are a realistic use case (for example, Playwright's HAR-based network mocking
   in Phase 4).
3. **Side findings.** The comment on line 12 says "JMeter plans", but the rules beneath it
   cover `*.jtl` (results) and `*.har` (browser traffic). `*.jtl` is ignored by `.gitignore`,
   so its rule only matters if a result file is force-added. `*.jmx` has no explicit rule and
   is correctly handled as text by `* text=auto eol=lf`.

### 13.4 Approved change (D5)

Revision 1 proposed removing the `*.har` text rule; the evidence supported the opposite, and
the following change is approved.

Lines 12–14 become:

```text
# HAR captures are JSON: normalize and diff them as text so reviewers can confirm they were
# sanitized (README §11). JMeter results (*.jtl) are ignored by .gitignore; this rule only
# applies if one is force-added. JMeter plans (*.jmx) are covered by `* text=auto eol=lf`.
*.jtl text eol=lf diff
*.har text eol=lf diff
```

Line 25 (`*.har binary`) is removed. No other line changes.

**Status:** applied in Task 7a (`c83768f`). Line 25 (`*.har binary`) was removed. At the user's
review request, the original comment on line 12 (`# JMeter plans are XML; diff them as text.`)
was kept unchanged instead of the replacement comment above; the `*.jtl` and `*.har` rules on
lines 13–14 were already as approved. `git check-attr` matches the expected result below.

**Expected result** (`git check-attr text eol diff merge binary -- sample.har`):
`text: set`, `eol: lf`, `diff: set`, `merge: unspecified`, `binary: unspecified`.

### 13.5 HAR sanitization stays mandatory

`.gitattributes` makes HAR content *visible* in review; it does not sanitize anything. The
README §11 rule — sanitize HAR files before committing — remains in force and is part of PR
review. No automated HAR scanner is added in Phase 1, because no HAR files exist yet; an
automated check is evaluated in Phase 4, when HAR fixtures are first introduced.

---

## 14. Consistency with ADRs and project structure

| Item | Status |
|---|---|
| ADR-0002 — single reactor, layered modules, centralized versions, Java 21, wrapper, independent execution, no placeholder modules | Consistent |
| ADR-0002 — architecture test for dependency direction | Deferred to Phase 2 (needs `frameworks/`) |
| ADR-0003 — nothing depends on `learning/`; opt-in profile for practice code | Consistent; no `learning/` code yet, profile added when the first code lands |
| ADR-0004 — SUT imports the Spring Boot BOM, not the Spring Boot parent | Consistent; layer table corrected — `sut/` is a module, not a layer parent (§3.3) |
| ADR-0005 — Docker-compatible configuration | Not exercised in Phase 1 (no containers) |
| ADR-0006 — one tool per responsibility; Checkstyle non-formatting only | Consistent; one justified clarification (`MultilineCommentLeadingAsteriskPresence`), recorded in ADR-0007 |
| ADR-0006 — coverage "on library modules (`platform`, `frameworks`)" | ADR-0006 unchanged. Phase 1 applies D3 to `platform` production code; `frameworks` thresholds deferred to Phase 2 (D6) |
| README repository structure | Updated to add `build-config/` and `docs/plans/` (§12, Task 7a) |
| `CLAUDE.md` rules (versions only in root POM, branch model, no credentials, no `Thread.sleep`) | Consistent. "Child POMs never declare versions" is now enforced by the build (D10). Commands updated in Task 7a (§12) |
| `CLAUDE.md` — "Read secrets from environment variables" | Consistent; enforced in code by D7 |

---

## 15. Implementation sequence

| # | Task | Exit criterion | Proposed commit message |
|---|---|---|---|
| 1 | Sync `main` with `origin/main` (`git switch main && git pull --ff-only`); create `feature/phase-1-build-backbone` | Branch points at `cdebdd1` (or newer `origin/main`) | — |
| 2 | Root POM, `platform/pom.xml`, enforcer rules | `./mvnw -B validate` passes on the empty reactor | `build: add root parent POM and platform layer` |
| 3 | Spotless, Checkstyle (+ suppressions), SpotBugs, JaCoCo | Formatter/Checkstyle acceptance test (§6.4) passes; gate negative checks fail as expected | `build: add Spotless, Checkstyle, SpotBugs and JaCoCo gates` |
| 4 | `test-core` with unit tests; R18 no-tests guard in `platform/pom.xml`; deferred Enforcer negative tests (R2) | Module `verify` green, coverage met; a platform module with production code but no `src/test/java` fails `verify`; `dependencyConvergence` and `banDynamicVersions` each fail on a deliberate violation | `feat(core): add layered test configuration and secrets` |
| 5 | `test-reporting` with unit and wiring tests | Module `verify` green, coverage met; SLF4J converged; Allure directory and `clean` checks pass (§8.6) | `feat(reporting): add Allure integration and sensitive-data masking` |
| 6 | CI workflow and Dependabot | actionlint clean on the committed file, including the step-id negative control | `ci: add GitHub Actions build and Dependabot` |
| 7 | ADRs 0007–0009, module READMEs, README, ADR index, `CLAUDE.md`, `.gitattributes` per §13.4 | All links resolve; `git check-attr` matches §13.4 | `docs: add Phase 1 ADRs and module documentation` |
| 8 | Full positive and negative validation; push and open PR; CI behavioural validation (§11.4) | Green CI on the PR; scenario truth table confirmed with run links | — |

**Status on 2026-11-07** (the table above is the approved plan and is kept unchanged):

| # | Status | Commit(s) on `feature/phase-1-build-backbone` |
|---|---|---|
| 1 | Done — branch created from `origin/main` at `cdebdd1` | — |
| 2 | Done | `8580b53` build: add root parent POM and platform layer |
| 3 | Done; committed under an approved, different message | `ff24d8a` build: enforce formatting, static analysis and coverage gates |
| 4 | Done | `5725b93` feat(core): add layered test configuration and secrets |
| 5 | Done, as two commits: the D7 breaking change to Task 4 behaviour was committed separately, as approved | `d9691cb` fix(core)!: accept secrets only from SECRET_ environment variables; `12d82e8` feat(reporting): add Allure integration and sensitive-data masking |
| 6 | Done | `8064973` ci: add GitHub Actions build and Dependabot |
| 7 | Done. **Redefined for this revision** as "finalize and relocate the Phase 1 plan"; the original Task 7 deliverables above moved to Task 7a (O4) | `9868919` docs(plans): add Phase 1 implementation plan |
| 7a | Done (O4) — the original Task 7 documentation deliverables; committed under an approved, different message | `c83768f` docs: add Phase 1 architecture and usage documentation |
| 7b | Done (O5) — the 16 references to the old plan path updated | `3306dc1` docs: update Phase 1 plan references |
| 8 | **Done** (2026-11-07). Step 1: full local validation and O6 evidence passed (§8.6, §10). The branch was pushed and merged through PR #2. Required check `Build and verify` set through the ruleset `Protect main` (§11.3). O3 Scenario A completed on PR #3, closed without merging (§11.4). Scenario B not executed: accepted residual risk (R14, R22). **Process deviation:** PR #2 was merged 12 seconds after it was opened, before its CI run completed (both runs later passed), and it used GitHub's default title and an empty description instead of the approved draft. `Protect main` now requires `Build and verify` to pass before merging, which prevents a repeat | `e0976ab` Merge pull request #2 (the O3 commits were never merged) |

Task 5's exit criterion "Allure directory and `clean` checks pass (§8.6)" is partly evidenced:
see the §8.6 status note and O6.

**Approved sequence and approval gates.** The steps run in this order. Each step needs its own
explicit approval, and approval of one step never authorizes another. No step authorizes a push,
pull request or merge unless that approval says so explicitly.

| Step | Action | Gate |
|---|---|---|
| 1 | Review and approve Task 7 (this plan) | Review by the user; explicit approval of Task 7 |
| 2 | Commit Task 7 (`docs/plans/phase-1.md` only) | Separate explicit approval to commit Task 7 |
| 3 | Commit Task 6 (`.github/workflows/ci.yml`, `.github/dependabot.yml`) | Separate explicit approval to commit Task 6 |
| 4 | Execute Task 7a (O4) | Separate explicit approval; steps 2 and 3 done |
| 5 | Review and complete Task 7a | Review by the user of the full Task 7a diff; any commit needs its own explicit approval |
| 6 | Execute Task 7b (O5) | Separate explicit approval; step 5 done |
| 7 | Review and complete Task 7b | Review by the user of the full Task 7b diff; any commit needs its own explicit approval |
| 8 | Start Task 8 | Separate explicit approval; steps 5 and 7 done |

**Progress on 2026-11-07:** steps 1–7 are done; each was approved separately and Tasks 7, 6, 7a
and 7b were committed in that order. Step 8 (Task 8) is done: each push, pull request and O3
operation was approved separately, and the branch was merged to `main` through PR #2.

**Order of dependencies.** Task 6 is committed before Task 7a (step 3 before step 4), so both
follow-up tasks start from a branch on which Task 6 is committed; in particular, Task 7b's change
to `.github/dependabot.yml` is a separate change on top of the committed Task 6 file. Task 7b
starts only after Task 7a is completed and reviewed (step 5 before step 6). Task 8 starts only
after both Task 7a and Task 7b are completed and reviewed (steps 5 and 7 before step 8).

**Task 7a — Phase 1 documentation (O4).**

| Item | Scope |
|---|---|
| Prerequisites | Task 7 approved and committed (steps 1–2); Task 6 committed (step 3); separate explicit approval to start (step 4). The task is complete only after the user's review (step 5) |
| Deliverables | Exactly the pending rows of §12: `docs/adr/0007-build-and-quality-gates.md`, `docs/adr/0008-test-configuration-and-secrets.md`, `docs/adr/0009-reporting-allure-and-masking.md` (each copied from `docs/adr/0000-template.md`), `platform/test-core/README.md`, `platform/README.md`, root `README.md`, `docs/adr/README.md`, `CLAUDE.md`, and the `.gitattributes` HAR change exactly as approved in D5 (§13.4). The S2–S4 findings (§7.8) and the R18 residual limitation are documented in the `test-core` README and ADR-0008 / ADR-0007 as listed in §12 |
| Constraints | Content follows the approved decisions D1–D10 and this plan; no new decision, version, tool or dependency. No Java, POM, workflow or Dependabot change |
| Validation | Every relative Markdown link in the new and changed files resolves; `git check-attr text eol diff merge binary -- sample.har` matches §13.4 (`text: set`, `eol: lf`, `diff: set`, `merge: unspecified`, `binary: unspecified`); `./mvnw -B -ntp clean verify` still passes |
| Review | All proposed diffs are shown in full and reviewed before any commit. The task does not authorize a commit or push |

**Task 7b — update references to the old plan path (O5).**

| Item | Scope |
|---|---|
| Prerequisites | Task 7 plan approved and committed (steps 1–2); Task 6 committed (step 3); Task 7a (O4) completed and reviewed (step 5); separate explicit approval to start Task 7b (step 6). The task is complete only after the user's review (step 7) |
| Baseline | **16 references** (approved baseline, confirmed by the user), re-scanned 2026-11-06 with `git grep -n --untracked 'PHASE-1-PLAN\.md'` (excluding this plan): `pom.xml` lines 49, 69, 161, 203, 215, 290, 301, 345; `platform/pom.xml` lines 3, 99; `platform/test-core/pom.xml` line 3; `platform/test-reporting/pom.xml` line 4; `platform/test-core/src/test/resources/logback-test.xml` line 2; `platform/test-reporting/src/test/resources/logback-test.xml` line 2; `build-config/checkstyle-suppressions.xml` line 8; `.github/dependabot.yml` line 1. Of these, 14 are comments and 2 are Maven Enforcer failure messages (`pom.xml:290`, `platform/pom.xml:99`) |
| Change | Replace `PHASE-1-PLAN.md` with `docs/plans/phase-1.md` in exactly those 16 places. Section references (§…, D2, R18) and all behaviour stay unchanged. If a fresh scan finds a different count or file list, the difference is reported for review before anything is changed; the scope is not expanded silently |
| Not in scope | Mentions of the old file name inside this plan (historical record) |
| Validation | `git grep -n --untracked 'PHASE-1-PLAN\.md'` returns matches only in `docs/plans/phase-1.md`; the diff changes only the path text (checked with `git diff --word-diff`); `./mvnw -B -ntp clean verify` passes |
| Review | The full diff is reviewed before any commit. The task does not authorize a commit or push |

**Task 8 — prerequisites.** The Task 8 scope is unchanged (row 8 of the approved table above,
§10, §11.4, O3, O6).

| Item | Requirement |
|---|---|
| Prerequisites | Task 6 committed (step 3); Task 7a completed and reviewed (step 5); Task 7b completed and reviewed (step 7); separate explicit approval to start Task 8 (step 8) |
| Within Task 8 | Pushing, opening the pull request and the temporary CI validation branch each need the approvals already required by §11.4, §16 and O3 |

---

## 16. Implementation controls

1. Local `main` is synced with `origin/main` before branching; work happens only on
   `feature/phase-1-build-backbone`.
2. **One task at a time.** After each task: changed files, explanation of the
   implementation, commands executed and validation results.
3. **No commit, push or pull request** without explicit approval. Commit messages in §15 are
   proposals.
4. Temporary negative-validation faults are reverted before reporting; they never reach a
   commit.
5. Downloaded validation tools (actionlint, probes) live in the session scratch directory,
   never in the repository or system paths.
6. Any deviation from this plan discovered during implementation is reported and approved
   before it is applied.
7. The temporary CI validation branch and draft PR (§11.4) are created only with explicit
   approval, never merged, and removed afterwards. Executed as a regular, unmerged PR (#3), for
   the reason given in §11.4; the remote branch was deleted and the local branch kept.

---

## 17. Risks and mitigations

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | **Resolved in Task 3; maintenance corrected in Task 7.** google-java-format 1.37.0 crashes inside Spotless 3.10.4 on every file (`InvocationTargetException`), even with the six `jdk.compiler` `--add-exports` flags applied via `.mvn/jvm.config` (verified to be loaded) or `MAVEN_OPTS`. Spotless 3.10.4's built-in default is 1.36.1, and with 1.36.1 formatting works **without** any JVM flags. **Dependabot is not expected to update google-java-format:** its version property is referenced inside the Spotless plugin's `<configuration>`, not declared as a Maven dependency. This expected limitation has not yet been confirmed by a Dependabot run on GitHub | Expected (not yet confirmed) | The formatter falls behind silently | Pinned **Spotless 3.10.4 + google-java-format 1.36.1**; no `.mvn/jvm.config`. **Until verified, google-java-format is maintained manually:** check it whenever Spotless is updated (including Dependabot's Spotless PRs); select only a release at least 14 days old (§4.2) that the Spotless version in use supports; repeat the §6.4 formatter and Checkstyle acceptance test before upgrading (R10). The first Dependabot run must explicitly check whether a google-java-format update is proposed (§11.5, R22). The current versions stay unchanged |
| R2 | Allure 3 is recent; most community examples target Allure 2 | Medium | Integration surprises | Compatibility verified (§5.2); wiring test proves results are produced; fallback to 2.35.5 via ADR if blocked |
| R3 | `-Xlint:all` with `failOnWarning` flags warnings such as `this-escape` or `serial` | Medium | Compilation fails | Fix the code; disable a specific lint only with inline justification |
| R4 | `dependencyConvergence` reports transitive conflicts | Medium | Build fails | Resolve by pinning in `dependencyManagement`, not by scattering exclusions |
| R5 | Coverage thresholds encourage shallow tests | Low | False confidence | Branch gate plus review of behavioural assertions |
| R6 | Listeners registered twice if a suite also declares them in `testng.xml` | Low | Duplicate log lines / writes | Auto-registration documented; listeners idempotent |
| R7 | `TestConfig.current()` misused as a service locator | Medium | Hidden coupling | Restricted to listeners (ADR-0008, `CLAUDE.md`) |
| R8 | `requireJavaVersion [21,22)` blocks contributors on newer JDKs | Low | Local build refused | Approved trade-off (D2); one-line change if revisited |
| R9 | Google Javadoc rules generate friction | Medium | Developer friction | Kept — public API documentation matters; tune only via ADR with evidence |
| R10 | Formatter/Checkstyle drift after an upgrade of either tool (Checkstyle through Dependabot; google-java-format by hand until Dependabot coverage is verified, R1) | Medium | New conflicts | Upgrade PR must pass `verify`; repeat the §6.4 acceptance test for formatter or Checkstyle upgrades |
| R11 | SHA pins drift out of date | Low | Missing fixes | Dependabot `github-actions` ecosystem |
| R12 | Selected versions superseded quickly | Certain | Minor drift | Dependabot with 14-day cooldown; grouped weekly PRs |
| R13 | `jacoco:report` on failure runs `--offline` | Low | Step fails if a plugin was not yet downloaded | The step now runs only when the Maven step itself ran and failed, so plugins were already resolved; a failure of this step never hides the original failure. Validated locally in Task 6 after a deliberate test failure (§11.4) |
| R14 | actionlint does not detect an invalid `outcome` literal or an `id` on the wrong step | Low | Coverage step silently never runs | Review plus behavioural validation on GitHub (§11.4). **Status (Task 8):** the success and test-failure paths are proven on GitHub (PR #2, PR #3): the coverage step was skipped on success and ran after a test failure. The setup-failure path (Scenario B) was not executed and is an **accepted residual risk** |
| R15 | Local `./mvnw verify` without `clean` accumulates Allure results | Medium | Stale results in local reports | Documented `./mvnw clean verify`; CI always starts clean; the §8.6 marker check was recorded in Task 8 step 1 (O6, done) |
| R16 | A future change drops the explicit `allure.results.directory` | Low | Results escape `target/` and are never cleaned | Wiring test asserts the exact directory and the absence of the fallback directory (§8.5) |
| R17 | **Known issue (accepted for Phase 1; revisit in Phase 2).** The Checkstyle plugin's dependency tree contains old `javax` artifacts whose parent POM `net.java:jvnet-parent:5` declares the retired repository `jvnet-nexus-releases` (`https://maven.java.net/...`). Maven's resolver probes it for `/.meta/prefixes.txt` and logs a `[WARNING]` (certificate failure) | Certain | Log noise; an outbound request to a dead domain. All artifacts still resolve from Maven Central; the build passes | No resolver settings changed in Phase 1 (decision, Task 3). Phase 2 evaluates a clean fix (for example a repository filter or mirror) |
| R18 | **Gap found in Task 3 — accepted temporarily (option a), fixed in Task 4 (option b).** A platform module with production code but **no tests** passes the coverage gate: no tests means no `jacoco.exec`, and `jacoco:check` skips instead of failing. `-Dsurefire.failIfNoTests=true` does **not** close it (Surefire returns early when `src/test` is absent) | Low (every planned platform module has tests) | D3 could be bypassed silently | **Implemented in Task 4:** `platform/pom.xml` profile `require-tests-for-production-code` (activated by `${basedir}/src/main/java`) runs Enforcer `requireFilesExist` on `src/test/java` in `validate`. Proven: a module with production code and no `src/test/java` fails; `test-core` passes; `platform-parent` is unaffected. **Residual limitation:** an *empty* `src/test/java` satisfies the guard (Git does not track empty directories, so this can only happen locally); code review remains the safeguard for test quality |
| R19 | **Found and fixed in Task 5.** Allure writes run-level errors (`*-globals.json`) without any listener callback; `AllureTestNg` does so on configuration-method and data-provider failures, copying the exception message and stack trace, so a secret in such a failure reached the results directory unmasked | — | — | **Fixed (Task 5 decision A; D9):** `MaskingResultsWriter` wraps Allure's `FileSystemResultsWriter` and masks every global error and global attachment name before writing, failing closed. `AllureLifecycleInstaller` installs it through the public `Allure.setLifecycle` API before TestNG constructs `AllureTestNg` (ServiceLoader order), and aborts the run if that order is violated. Proven by unit, recording-writer and end-to-end tests with real configuration and data-provider failures, a negative control without the installer and a wrong-order control |
| R20 | **Accepted with documentation (Task 5 decision B; D10).** `banDynamicVersions` skips an allowlisted artifact entirely, whatever its version, so a range on `test-core` or `test-reporting` (for example `[0.1.0-SNAPSHOT,)`) passes that rule. Writing the ignore as `groupId:artifactId:${project.version}` does not help: the range resolves to that same version | Low | A range on an internal module would not be caught by that rule | Enforcer `evaluateBeanshell` rule: no child POM may declare a dependency version at all (`<dependencies>`, `<dependencyManagement>`, profiles, plugin dependencies); the root `dependencyManagement` pins internal modules to `${project.version}`. Proven: explicit `${project.version}`, a range on `test-core`, an external version and a child `dependencyManagement` entry each fail; a version inside an XML comment passes. The root POM itself remains protected by review |
| R21 | **Accepted with safeguards (Task 5 decision C).** Surefire and Failsafe copy every system property into their XML reports before any test runs. A secret passed as `-Dsecret.*` is therefore recorded in that run's XML even though `ConfigLoader` rejects it and the run fails | Low | Secret exposed in a local or uploaded report | Secrets are accepted only from `SECRET_*` environment variables (D7); the README states that a secret passed with `-D` must be treated as exposed and rotated; every CI workflow maps secrets through `env:` only (§11.3) |
| R22 | **Found in Task 6.** Static validation cannot prove CI behaviour: actionlint misses a wrong `outcome` literal (R14), the Dependabot schema accepts a group without `patterns`, and Dependabot's real coverage (google-java-format, Maven Wrapper) is known only after it runs on GitHub | Medium | A misconfiguration is discovered late | Review; behavioural CI runs in Task 8 (§11.4); first Dependabot run checked against §11.5, including explicitly whether a google-java-format update is proposed (R1). **Status (Task 8):** green and test-failure CI behaviour proven on GitHub; Scenario B (setup failure) not executed, an **accepted residual risk**; first Dependabot run checked (§11.5): no google-java-format PR, supporting but not conclusively confirming R1; Maven Wrapper coverage still unverified |

---

## 18. Decision log and open items

| # | Item | Status |
|---|---|---|
| O1 | `.gitattributes` `*.har` | **Approved** — recorded as D5 (§13.4) |
| O2 | `frameworks` coverage thresholds | **Deferred to Phase 2** — recorded as D6 |
| O3 | Temporary CI validation branch and draft PR (§11.4) | **Scenario A done** in Task 8: PR #3 (regular, not draft), runs 42295149608 (failure) and 42296537859 (green); merge blocking confirmed by the repository owner in the GitHub UI; closed without merging. **Scenario B not executed: accepted residual risk** (R14, R22) |
| O4 | The original Task 7 deliverables (§12, §13.4): ADRs 0007–0009, `test-core` README, `platform/README.md`, `README.md`, `docs/adr/README.md`, `CLAUDE.md`, `.gitattributes` D5, S2–S4 and R18 documentation | **Done in Task 7a** (`c83768f`), after the Task 6 commit and before Task 8, as approved (§15, steps 4–5). Scope, validation and review requirements in §15. They remain part of the §19 definition of done |
| O5 | References to `PHASE-1-PLAN.md`, which no longer exists at that path, in `pom.xml` (8), `platform/pom.xml` (2), `platform/test-core/pom.xml`, `platform/test-reporting/pom.xml`, both `logback-test.xml` files, `build-config/checkstyle-suppressions.xml` and `.github/dependabot.yml`: **16 references** (14 comments and 2 Enforcer failure messages). This is the approved baseline, confirmed by the user; an earlier review package miscounted it as 17. All cited section numbers still exist | **Done in Task 7b** (`3306dc1`), a separate, narrowly scoped change, as approved (§15, steps 6–7). All 16 references now point to `docs/plans/phase-1.md`; only this plan still names the old file, as its historical record. The `.github/dependabot.yml` change is separate from the Task 6 commit |
| O6 | §8.6 marker and `clean` checks not evidenced in the Task 5 report | **Done** in Task 8 step 1 (2026-11-07); results in §8.6 |

Revision 4 introduces no new technical decision. The scopes of O4 and O5 were approved as
follow-up work after the Task 7 review; neither is executed in Task 7, and neither may start
without its own explicit approval. Revision 4 was approved and committed in Task 7; both
follow-up tasks are now done (status on 2026-11-07).

---

## 19. Definition of done

- `./mvnw -B verify` passes locally and in GitHub Actions.
- Each platform module builds independently.
- All four quality gates are enforced and proven to fail on violations; formatter output passes
  Checkstyle with no formatting conflicts.
- `test-core` and `test-reporting` meet D3 thresholds with behavioural tests.
- No credentials in the repository; `secret.*` in YAML is rejected by code.
- CI uploads test, coverage and analysis reports on success and failure; the coverage-on-failure
  step runs only when the Maven step failed, proven by the scenario runs (§11.4).
- Allure results are written only to `target/allure-results`, and `clean` removes stale results
  (§8.6).
- `.gitattributes` treats HAR files as diffable text (§13.4).
- ADRs 0007–0009 and module READMEs are written; all links resolve.
- Pull request reviewed and merged; `Build and verify` (displayed by GitHub as
  "CI / Build and verify") set as a required check.

**Status on 2026-11-07** (closeout; criteria above unchanged except for the exact check name):

| Criterion | Status |
|---|---|
| `./mvnw -B verify` locally / in GitHub Actions | Done: locally green (Task 8 step 1); GitHub runs 42279950007 and 42279970970 green |
| Each platform module builds independently | Done: exercised with `-pl … -am` during Tasks 4–5 and recorded again in Task 8 step 1 |
| Four gates enforced, proven to fail; no formatter/Checkstyle conflict | Done (Task 3) |
| D3 thresholds met with behavioural tests | Done: `test-core` 98.3% lines / 92.5% branches; `test-reporting` 96.1% / 95.3% |
| No credentials; `secret.*` rejected in YAML | Done; also rejected as system properties (D7) |
| CI uploads reports; coverage step only after a Maven failure | Done for success and test failure (§11.4: reports uploaded in both; coverage step skipped on success, ran after the test failure). Not independently verified: JaCoCo HTML inside the failure artifact. Setup-failure path: accepted residual risk (R14, R22) |
| Allure results only in `target/allure-results`; `clean` removes stale results | Done: directory asserted by tests; `clean` evidence recorded (O6, §8.6) |
| `.gitattributes` HAR change | Done (Task 7a, §13.4) |
| ADRs 0007–0009 and module READMEs; links resolve | Done (Task 7a; `test-reporting` README in Task 5) |
| PR merged; required check set | Done: PR #2 merged (`e0976ab`), with the process deviation recorded in §15 (merged before CI completed); `Protect main` requires `Build and verify` (§11.3) |

**Phase 1 closure:** every criterion above is met or has an accepted residual risk. Phase 1 is
not yet formally closed: closure is pending the merge of the closeout documentation PR and a
final verification.
