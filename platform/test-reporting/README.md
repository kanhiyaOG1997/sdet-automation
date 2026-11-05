# test-reporting

Allure reporting for TestNG suites, with masking of sensitive data in log lines, report
attachments, the report's environment information, and the test results, containers and run-level
errors Allure writes.

Depends only on [`test-core`](../test-core/) (`test-reporting → test-core`).

## Components

| Class | Responsibility |
|---|---|
| `SensitiveDataMasker` | Replaces sensitive data in text with `****`, by secret value and by sensitive key name |
| `ReportAttachments` | Static `text`, `json` and `png` methods that attach content to the running test. Text and names are always masked first; there is no unmasked variant |
| `AllureEnvironmentListener` | TestNG `IExecutionListener`; writes `environment.properties` (environment name, non-secret configuration, Java version, OS name and architecture) into the Allure results directory at the start of a run |
| `TestLoggingListener` | TestNG `ITestListener`; logs `START`, `PASS`, `FAIL` and `SKIP` with durations, and puts the test name into the SLF4J MDC under `testName` |
| `AllureResultMasker` | Allure `TestLifecycleListener` and `ContainerLifecycleListener`; masks every text field of a test result or container (failure messages, stack traces, parameters, steps, fixtures, links, labels, descriptions, attachment names) immediately before Allure writes it |
| `MaskingResultsWriter` | Wraps Allure's results writer; masks run-level errors (`*-globals.json`: message, stack trace, actual and expected values, global attachment names) immediately before writing them |
| `AllureLifecycleInstaller` | TestNG listener that installs an Allure lifecycle writing through `MaskingResultsWriter`, before Allure's own TestNG listener is created; aborts the run if that order cannot be guaranteed |

## Usage

Add the dependency. Version and scope come from the root POM:

```xml
<dependency>
  <groupId>io.github.kanhiyaog1997.sdet</groupId>
  <artifactId>test-reporting</artifactId>
</dependency>
```

Nothing else is needed: TestNG discovers the listeners automatically (see below). In tests:

```java
ReportAttachments.text("request", requestDump);    // masked plain text
ReportAttachments.json("response", responseBody);  // pretty-printed, then masked
ReportAttachments.png("screenshot", bytes);        // image bytes cannot be masked
```

Run with `./mvnw clean verify` when you intend to generate a report: Allure never deletes old
results, so `verify` without `clean` accumulates results from earlier runs in
`target/allure-results` (CI always starts from a clean checkout).

This module's own tests run deliberately failing sample classes to prove the masking. They move
the samples' output to `target/allure-sample-results/` instead of deleting it, so it stays out of
the module's Allure report but can still be inspected and scanned.

## Listener discovery

`src/main/resources/META-INF/services/org.testng.ITestNGListener` registers
`AllureLifecycleInstaller`, `AllureEnvironmentListener` and `TestLoggingListener` through Java's
`ServiceLoader`. The
`allure-testng` jar registers Allure's own `AllureTestNg` the same way, so it is deliberately not
listed again. Do not declare these listeners again in `testng.xml` or with `@Listeners`: it is
unnecessary. (Adding a second instance with `TestNG#addListener` was verified not to duplicate log
lines; other ways of registering them twice have not been tested.)

Allure discovers `AllureResultMasker` the same way, through
`META-INF/services/io.qameta.allure.listener.TestLifecycleListener` and
`META-INF/services/io.qameta.allure.listener.ContainerLifecycleListener`. It runs in
`beforeTestWrite` and `beforeContainerWrite`, the last callbacks before Allure persists a result.

### Run-level errors and listener order

Allure writes run-level errors (`*-globals.json`) without any listener callback; `AllureTestNg`
does so when a configuration method such as `@BeforeMethod` fails and when a data provider fails.
Allure 3.0.0 also has no extension point for its results writer. The supported way to mask these
errors is therefore to give Allure a lifecycle whose writer masks them
(`Allure.setLifecycle(new AllureLifecycle(writer))`), which `AllureLifecycleInstaller` does once
per JVM.

`AllureTestNg` keeps the lifecycle that is current when TestNG constructs it, so the installer
must be constructed first. TestNG constructs ServiceLoader listeners in class-path order: the
installer is the first entry in this module's service file, and this module comes before
`allure-testng` because `allure-testng` is one of its dependencies. **Do not declare
`allure-testng` directly** in a consuming module; `test-reporting` already provides it. If the
order is ever wrong, the installer throws while TestNG loads its listeners, so the run fails
instead of writing unmasked errors.

## Supplying secrets

Secrets come **only** from `SECRET_*` environment variables, for example `secret.admin-password`
from `SECRET_ADMIN_PASSWORD`. A `secret.*` system property (`-Dsecret.x=…`) makes configuration
loading fail: system properties are visible in process listings, are saved in shell history and
are copied into Surefire XML reports. Note that the rejection cannot undo that copy: by the time
`ConfigLoader` rejects the property, Surefire or Failsafe has already recorded it in the XML report
of that run, so a secret passed with `-D` must be treated as exposed and rotated.

In CI, every workflow maps secrets through the job's or step's `env:` block, never on the Maven
command line and never as `-Dsecret.*`:

```yaml
- name: Build and verify
  env:
    SECRET_ADMIN_PASSWORD: ${{ secrets.ADMIN_PASSWORD }}
  run: ./mvnw --batch-mode verify
```

## How masking works

Two layers, applied in this order:

1. **By value.** Every occurrence of a configured secret is replaced. The secrets are
   `TestConfig.secretValues()`: the environment variables named `SECRET_*`. Matching is literal (regex-special and Unicode characters are safe), and overlapping
   occurrences are merged so no fragment of a secret survives.
2. **By key name.** Values following a key that contains `authorization`, `cookie`, `password`,
   `passwd`, `token`, `secret`, `api-key`/`api_key`/`apikey` or `credential` are masked in header
   lines (`Authorization: …`), JSON members (`"accessToken": "…"`) and `key=value` text (query
   strings, form bodies, log text). `Bearer` tokens are masked anywhere.

Masking always happens before content leaves the process: attachment text and names before the
Allure call, environment values before the file is written, log arguments before the log call,
test results and containers in Allure's last callback before they are written, and run-level
errors in the results writer immediately before they are written.

Masking **fails closed**: Allure catches and logs any exception a listener throws and then writes
the result anyway, so if masking fails the masker itself replaces every maskable text field of the
result, container or run-level error with `<masking failed>`. Identifiers, relationships and
metadata (`uuid`, `historyId`, `testCaseId`, container children, timestamps, status flags,
attachment sources) are never changed.

### Four-character minimum

Secrets shorter than **four characters** are not masked by value: replacing every `a`, `ab` or
`abc` in a report would corrupt ordinary text. The masker logs how many configured secrets were
skipped (never their values), and key-based masking still applies to them, so `password=abc` is
still masked. Use secrets of realistic length.

## Security limitations

Masking reduces the risk of leaks; it cannot guarantee that a secret never appears.

- **Only registered secrets are masked by value.** A credential stored under a non-`secret.*` key,
  or an environment variable such as `AWS_SECRET_ACCESS_KEY`, is plain configuration as far as
  `test-core` is concerned; only the key-based rules can catch it.
- **Encoded forms are not recognised**: URL-encoded, Base64-encoded or hashed secrets pass
  through, unless they follow a sensitive key name.
- **Short secrets** (fewer than four characters) are masked by key name only.
- **Images cannot be masked.** A screenshot that shows a secret contains it.
- **Explicit `Secret.reveal()` bypasses everything.** If a caller reveals a secret and passes it
  to an unrelated logger, assertion message or exception, this module cannot intercept it. Never
  use `reveal()` for logging or reporting; `Secret.toString()` already prints `****`.
- **Direct Allure attachments are not masked.** Allure writes attachment content at the moment it
  is attached; `AllureResultMasker` can only mask the attachment's name afterwards. Attach through
  `ReportAttachments`, never through `Allure.attachment(...)` or Allure's own HTTP filters.
- **Never pass secrets as data-provider parameters.** Allure derives `historyId` from parameter
  values before any listener runs; the value is masked in the result, but `historyId`, which is
  deliberately left unchanged, is a hash of it. Pass a key such as `"secret.admin-password"` and
  resolve the secret inside the test.
- **Surefire and Failsafe XML reports record failure messages and stack traces as they are.** No
  listener of this module reaches them. Never put secrets into assertion or exception messages.
- **Surefire and Failsafe XML reports record every system property.** A secret passed as
  `-Dsecret.*` is therefore written into the XML report of that run, even though configuration
  loading then rejects it and the run fails. Rejection prevents the secret from being used, not
  from being recorded. Supply secrets only as environment variables.
- **Global attachment content is not masked**, only its name, for the same reason as direct Allure
  attachments.
- **Key-based rules err towards masking.** Any value whose key *contains* a sensitive word is
  masked, for example `secretary: Alice` in a `key: value` line.
