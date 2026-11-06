# ADR-0008: Layered test configuration with environment-only secrets

- **Status:** Accepted
- **Date:** 2026-11-06

## Context

Every framework from Phase 2 onward needs base URLs, timeouts, browser choices and credentials
per environment. The project rules forbid credentials in code or configuration and require
secrets to be read from environment variables (`CLAUDE.md`). Configuration must be overridable
in CI without editing files. It must also be testable without mutating the process environment.
Finally, a secret must not leak through logs, reports or error messages
([Phase 1 plan](../plans/phase-1.md) §7).

## Decision

`platform/test-core` provides `TestConfig`, an immutable, thread-safe configuration view.

**Layers**, lowest to highest precedence, for ordinary keys:

```
classpath:config/default.yml  <  classpath:config/<env>.yml  <  environment variables  <  -D system properties
```

- **Environment selection:** `-Dtest.env` or `TEST_ENV`; default `local`. Names are lower-case
  letters, digits and dashes. An explicitly selected environment whose file is missing fails.
- **One key-mapping rule:** YAML `api.base-url` ⇔ `-Dapi.base-url` ⇔ `API_BASE_URL` (upper
  case; dots and dashes become underscores). Nested YAML is flattened to dotted keys. Lists and
  duplicate keys are rejected.
- **Typed access:** `require`, `find`, `requireInt`, `requireBoolean`, `requireDuration`,
  `requireUri`, `requireSecret`. A missing key fails with a `ConfigException` that names every
  source consulted.

**Secrets (D7):** keys in the `secret.` namespace come **only** from `SECRET_*` environment
variables (`secret.admin-password` ⇔ `SECRET_ADMIN_PASSWORD`), and are read only through
`requireSecret`, which returns a `Secret`. A `secret.*` key in a YAML file, or a `-Dsecret.*`
system property, makes loading fail. The message names the key or property and the environment
variable to use, never the value. `Secret.toString()` returns `****`; the raw value is available
only through an explicit `reveal()`.

**`TestConfig.current()`** is a lazily loaded, cached instance. It exists only for code that
TestNG creates through no-argument constructors, such as listeners. All other code receives a
`TestConfig` explicitly.

**Reporting support:** `nonSecretEntries()` (YAML keys with their effective values) feeds report
environment information. `secretValues()` (the non-empty `SECRET_*` values) feeds masking
([ADR-0009](0009-reporting-allure-and-masking.md)).

## Consequences

- The same YAML files work locally and in CI. CI overrides ordinary keys with environment
  variables or `-D`, and maps secrets through the workflow's `env:` block only.
- Tests inject environment variables and system properties as maps, so they never touch the real
  process environment.
- **Breaking change in Task 5 (D7):** Task 4 had accepted secrets as system properties; that
  path was removed.

**Security findings and limitations** (plan §7.8, R21):

- **S1 (fixed):** an invalid-YAML error could echo file content, including a secret, through the
  parser's message. The message now reports only the resource and the line and column, with no
  cause attached.
- **S2 (by design):** protection depends on naming. A credential stored under a key outside the
  `secret.` namespace (for example `db.password`), or an environment variable such as
  `AWS_SECRET_ACCESS_KEY`, is ordinary configuration. It is not in `secretValues()`, so it is not
  masked by value; only the reporting module's key-name rules can catch it. Put every credential
  under `secret.*`.
- **S3 (usage rule):** `Secret.reveal()` inside an assertion or exception message prints the
  value when the assertion fails. Never use `reveal()` for logging, reporting or assertion
  messages.
- **S4 (implemented in `test-reporting`):** secrets shorter than four characters are not masked
  by value, to avoid corrupting ordinary text; key-based masking still applies.
- **R21:** Surefire and Failsafe copy every system property into their XML reports before any
  test runs. A secret passed with `-Dsecret.*` is therefore recorded in that run's report even
  though loading rejects it. Such a secret must be treated as exposed and rotated.
- **R7:** `current()` could be misused as a service locator; it is restricted to listeners by
  convention and review.

## Alternatives considered

- **Owner library:** rejected; unmaintained. Jackson YAML is maintained and already in the
  dependency set.
- **Secrets in YAML files, possibly encrypted:** rejected; a file in the repository is the
  wrong place for a credential, and key management would add complexity without need.
- **Secrets as system properties** (the Task 4 behaviour): rejected in Task 5 (D7). System
  properties appear in process listings and shell history, and are copied into Surefire and
  Failsafe XML reports.
- **A global singleton for all code:** rejected; explicit injection keeps dependencies visible
  and tests simple. `current()` is the narrow exception for listeners.
