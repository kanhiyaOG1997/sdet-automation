# ADR-0007: Enforce build rules and quality gates in layered parent POMs

- **Status:** Accepted
- **Date:** 2026-11-06

## Context

Phase 1 creates the build backbone that every later module inherits
([Phase 1 plan](../plans/phase-1.md) §3–§6). [ADR-0002](0002-maven-multi-module-layout.md)
requires one reactor with centralized versions, and [ADR-0006](0006-code-quality-toolchain.md)
assigns one responsibility to each quality tool. Phase 1 has to turn both into rules that the
build enforces:

- Java 21 only, identical locally and in CI.
- No unpinned, dynamic or duplicated versions, and no versions in child POMs.
- Formatting, style rules, static analysis and coverage gates that never contradict each other.
- Different rules per layer: library code is coverage-gated, executable suites and the SUT are
  not.

## Decision

**Layered parent POMs.** The root `pom.xml` (`sdet-automation-parent`) is parent and aggregator.
It holds every dependency and plugin version (`dependencyManagement`, `pluginManagement`) and the
quality gates for all modules, but declares no `<dependencies>`. A thin layer parent adds rules
for one layer only. `platform/pom.xml` (`platform-parent`) is the only one in Phase 1. Further
layer parents are created with their first module (plan §3.3).

**Version policy.** Every version is the newest stable release published at least 14 days before
selection; pre-releases are never used (plan §4.2). Dependabot applies the same rule with a
14-day cooldown. google-java-format is referenced inside the Spotless plugin configuration, not
declared as a Maven dependency, so Dependabot is not expected to update it. This has not yet been
confirmed by a Dependabot run. Until it is, google-java-format is maintained manually: check it
whenever Spotless is updated, apply the 14-day rule, and repeat the formatter and Checkstyle
acceptance test (plan §6.4) before upgrading (plan R1).

**Maven Enforcer rules** (bound to `validate`, plan §4.5):

| Rule | Setting |
|---|---|
| `requireJavaVersion` | `[21,22)` (D2) |
| `requireMavenVersion` | `[3.9.0,)`; the Maven Wrapper is pinned to 3.10.0 |
| `requirePluginVersions` | Every plugin pinned |
| `dependencyConvergence` | Conflicting transitive versions fail the build |
| `banDuplicatePomDependencyVersions` | Rejects duplicate declarations, which Maven 3.9.x only warns about |
| `banDynamicVersions` | Forbids `LATEST`, `RELEASE`, ranges and SNAPSHOTs, with an exact allowlist of this reactor's own modules `io.github.kanhiyaog1997.sdet:test-core` and `:test-reporting` (D10) |
| `evaluateBeanshell` | No `<version>` on any dependency declared in a child POM (dependencies, dependency management, profiles, plugin dependencies); the root POM is exempt (D10) |

`platform/pom.xml` adds the profile `require-tests-for-production-code`: in a module that has
`src/main/java`, Enforcer `requireFilesExist` requires `src/test/java` (plan R18).

**Quality gates** (bound to `verify`, in this order, plan §4.6 and §6):

| Gate | Configuration |
|---|---|
| Spotless 3.10.4 + google-java-format 1.36.1 | Sole owner of Java formatting, style `GOOGLE`, `reflowLongStrings=true`, `formatJavadoc=true` |
| Checkstyle 14.3.0 | The bundled, unmodified `google_checks.xml`, plus [`build-config/checkstyle-suppressions.xml`](../../build-config/checkstyle-suppressions.xml), which suppresses the 33 layout modules (41 entries) that google-java-format guarantees. Java sources only, test sources included, `violationSeverity=warning` |
| SpotBugs 4.10.5 | Effort `Max`, threshold `Medium`, main sources |
| JaCoCo 0.8.15 | Agent and report in every module; the check runs only in `platform/pom.xml`: **line ≥ 80%, branch ≥ 70%** of platform production code, at bundle level (D3) |

The compiler uses `release 21`, `-Xlint:all,-processing` and `failOnWarning=true`.
`./mvnw verify` is the complete gate and the exact command CI runs.

## Consequences

- One command, `./mvnw clean verify`, proves a change. A formatting failure is fixed with
  `./mvnw spotless:apply`. Every other gate is fixed in the code.
- Formatter output passes Checkstyle with no formatting conflicts. This was proven in Task 3
  through the real Maven configuration (plan §6.2 and §6.4). `MultilineCommentLeadingAsteriskPresence`
  stays active because the formatter does not own it: a clarification of ADR-0006.
- Upgrading Checkstyle or google-java-format means repeating the §6.4 acceptance test
  (plan R10).
- `requireJavaVersion [21,22)` refuses newer JDKs (plan R8, approved trade-off).
- Coverage thresholds for `frameworks/` are deferred to Phase 2 (D6). Test code, `sut/`,
  `test-suites/` and `learning/` are never coverage-gated (D3).

**Known limitations:**

- **R18 residual limitation:** an *empty* `src/test/java` directory satisfies the
  no-tests guard. Git does not track empty directories, so this can happen only locally; code
  review remains the safeguard for test quality.
- **R20:** `banDynamicVersions` skips an allowlisted artifact whatever its version, so a range on
  `test-core` would pass that rule. The child-POM version rule catches it instead, because child
  POMs may not declare versions at all. The root POM is protected by review.
- The `evaluateBeanshell` rule is a text match, not an XML parse: Beanshell cannot use the JDK's
  internal XML parser on Java 21.
- **R17:** the build logs a known `[WARNING]` about the retired `jvnet-nexus-releases`
  repository, probed through an old Checkstyle-plugin dependency. Accepted for Phase 1.

## Alternatives considered

- **One parent POM with profiles per layer:** rejected; conditional build logic is harder to
  read than a thin layer parent.
- **Gates bound to `test` or `compile`:** rejected; an unformatted file would block running tests
  mid-change. `verify` still gates every merge.
- **Editing `google_checks.xml`:** rejected; a suppression file keeps the upstream configuration
  unmodified, so Checkstyle upgrades carry Google's rule improvements.
- **Dropping `banDuplicatePomDependencyVersions`** because Maven 3.10 rejects duplicates itself:
  rejected; builds that bypass the wrapper with Maven 3.9.x only warn (plan §4.5).
- **google-java-format 1.37.0:** rejected; it crashes inside Spotless 3.10.4 (plan R1).
- **Allowlisting the whole `io.github.kanhiyaog1997.sdet` group in `banDynamicVersions`:**
  rejected; the allowlist names exactly the reactor's own modules (D10).
