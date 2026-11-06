# ADR-0009: Allure 3 reporting with masking at write time

- **Status:** Accepted
- **Date:** 2026-11-06

## Context

Every suite needs one consistent reporting integration, and the README security rules require
sensitive data to be redacted from logs and reports. Tests will attach requests, responses and
screenshots, and failure messages can contain tokens. Allure writes results to disk itself,
through its own TestNG listener. Masking therefore has to happen inside Allure's write path, not
only in our own helper methods ([Phase 1 plan](../plans/phase-1.md) §5 and §8).

## Decision

**Allure Java 3.0.0 (D4)** via `allure-bom`. It is the newest release older than 14 days, is
compiled for Java 17, and was built and tested against exactly our TestNG 7.12.0 (plan §5.2).
`allure-testng` declares no TestNG dependency, so `test-reporting` declares TestNG at compile
scope.

**No AspectJ agent.** `@Step` and `@Attachment` need a load-time weaver. The `Allure.step(...)`
lambda API and `ReportAttachments` cover the same needs without one.

**Results directory:** Surefire sets
`allure.results.directory=${project.build.directory}/allure-results`, an absolute path. Allure's
default, `allure-results`, is relative to the working directory, outside `target/` and not
removed by `clean`. `test-reporting` ships no `allure.properties`, so it imposes nothing on
consuming suites. Allure never deletes old results, so `./mvnw clean verify` is the command to
use before generating a report.

**Listener discovery:** `test-reporting` registers its TestNG listeners and Allure lifecycle
listeners through `META-INF/services`. Consumers need no configuration.

**Masking in layers**, all through `SensitiveDataMasker`. It masks by value (every
`TestConfig.secretValues()` entry of at least four characters, literal match, overlaps merged)
and then by key name (`authorization`, `cookie`, `password`, `passwd`, `token`, `secret`,
`api-key`/`api_key`/`apikey`, `credential`), plus `Bearer` tokens:

1. **`ReportAttachments`** masks text and names before calling Allure; there is no unmasked
   variant.
2. **`AllureEnvironmentListener`** writes `environment.properties` from non-secret configuration,
   masked.
3. **`TestLoggingListener`** masks failure messages before logging them.
4. **`AllureResultMasker` (D8)** masks every text field of test results and containers,
   recursively through steps and fixtures, in `beforeTestWrite` / `beforeContainerWrite`. These
   are the last callbacks before Allure writes them.
5. **`MaskingResultsWriter` (D9)** wraps Allure's `FileSystemResultsWriter` and masks run-level
   errors (`*-globals.json`) and global attachment names. Allure writes these without any listener
   callback, for example when a configuration method or a data provider fails.
   **`AllureLifecycleInstaller`** installs it through the public
   `Allure.setLifecycle(new AllureLifecycle(writer))` API, once per JVM, before TestNG constructs
   `AllureTestNg`. No reflection and no internal Allure API is used.

Masking **fails closed**. If masking throws, every maskable text field is replaced with
`<masking failed>`. Identifiers, `historyId`, relationships and attachment sources are never
changed.

Report generation (`allure-maven` 3.1.0) and publishing are deferred to Phase 2.

## Consequences

- Secrets in failure messages, stack traces, parameters, steps, attachment names and run-level
  errors are masked before they reach `target/allure-results`. Unit, recording-writer and
  end-to-end tests prove this with real configuration-method and data-provider failures
  (plan §8.5, R19).
- **Listener order matters.** `AllureTestNg` keeps the lifecycle that is current when it is
  constructed. TestNG constructs ServiceLoader listeners in class-path order, so the installer is
  first in the service file, and `test-reporting` precedes `allure-testng` on the class path.
  If the order is wrong (for example, a module declares `allure-testng` directly ahead of
  `test-reporting`), the installer throws while TestNG loads its listeners. The run fails instead
  of writing unmasked errors. Consumers must not declare `allure-testng` directly.
- The wiring test asserts the exact results directory and the absence of the fallback directory,
  so a dropped Surefire setting fails the build (plan R16).

**Known limitations** (documented in the [module README](../../platform/test-reporting/README.md)):

- **Attachment content written directly through Allure is not masked.** Allure writes it at
  the moment of attaching, and the masker can only mask the attachment's *name* afterwards. This
  covers `Allure.attachment(...)`, Allure's own HTTP filters, and global attachments. Attach
  through `ReportAttachments`.
- **Data-provider parameters:** Allure derives `historyId` from parameter values before any
  listener runs. The parameter value is masked, but `historyId`, deliberately left unchanged, is
  a hash of it. Never pass secrets as data-provider parameters; pass a key and resolve the secret
  inside the test.
- **Surefire and Failsafe XML** record failure messages, stack traces and every system property
  as they are; no listener reaches them (R21).
- Images, encoded secrets (URL, Base64, hashes) and secrets shorter than four characters are not
  masked by value. Short secrets are still masked by key name.
- `Secret.reveal()` passed to an unrelated logger or message bypasses every layer.
- Stale results accumulate in `target/allure-results` when `verify` runs without `clean`
  (plan R15).

## Alternatives considered

- **Allure 2.35.5:** rejected; 3.0.0 is eligible, built against our TestNG version, and 2.x
  offers no advantage for a new codebase. 2.35.5 stays the documented fallback (plan R2).
- **AspectJ annotations:** rejected for Phase 1; it adds an agent for no functional gain.
- **Masking only in `ReportAttachments`:** rejected; failure messages, parameters and run-level
  errors are written by Allure itself.
- **A custom results writer through reflection or an internal Allure API:** rejected; Allure
  3.0.0 has no writer extension point, and `Allure.setLifecycle` is the supported public route.
- **An `allure.properties` file in `test-reporting`:** rejected; a library resource would impose
  its value on every consuming suite.
