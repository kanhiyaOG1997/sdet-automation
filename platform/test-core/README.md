# test-core

Typed, layered test configuration and safe secret handling. Every framework and suite reads
its settings, such as base URLs, timeouts, browser choices and credentials, through this module.
Design and rationale: [ADR-0008](../../docs/adr/0008-test-configuration-and-secrets.md).

Package `io.github.kanhiyaog1997.sdet.core.config`. Dependencies: Jackson (YAML parsing) and
`slf4j-api`. No logging backend is shipped.

## Components

| Class | Responsibility |
|---|---|
| `TestConfig` | Immutable, thread-safe configuration view with typed accessors, `requireSecret`, `nonSecretEntries()` and `secretValues()` |
| `Secret` | A sensitive value: `toString()` returns `****`; the raw value only through `reveal()` |
| `ConfigException` | The single unchecked exception for missing or invalid configuration |
| `ConfigLoader` (package-private) | Merges the layers and rejects secrets in YAML files and system properties |

## Usage

Add the dependency. Its version comes from the root POM:

```xml
<dependency>
  <groupId>io.github.kanhiyaog1997.sdet</groupId>
  <artifactId>test-core</artifactId>
</dependency>
```

This module ships no configuration files. The consuming module provides them on its classpath,
usually under `src/test/resources`:

```yaml
# config/default.yml: shared defaults
api:
  base-url: http://localhost:8080
  timeout: 30s
```

```java
TestConfig config = TestConfig.load();              // or receive one explicitly
URI baseUrl = config.requireUri("api.base-url");
Duration timeout = config.requireDuration("api.timeout");
Secret password = config.requireSecret("secret.admin-password");
```

`TestConfig.current()` returns one cached instance per JVM. It exists only for code that TestNG
creates through no-argument constructors, such as listeners. Everything else should receive a
`TestConfig` explicitly.

## Configuration keys and precedence

Highest precedence first:

1. JVM system property: `-Dapi.base-url=…`
2. Environment variable: `API_BASE_URL` (upper case; dots and dashes become underscores)
3. `classpath:config/<env>.yml`
4. `classpath:config/default.yml`

| Item | Rule |
|---|---|
| Environment | `-Dtest.env=staging` or `TEST_ENV=staging`; default `local`. Lower-case letters, digits and dashes. An explicitly selected environment whose file does not exist fails |
| YAML | Nested keys are flattened to dotted keys (`api.base-url`). Lists and duplicate keys are rejected. Invalid YAML fails with the resource name and line and column only, never the parser's message (S1) |
| Accessors | `require`, `find`, `requireInt`, `requireBoolean` (`true`/`false`), `requireDuration` (`500ms`, `30s`, `5m`, `2h` or ISO-8601 `PT30S`), `requireUri` (absolute), `requireSecret` |
| Missing key | `ConfigException` naming every source consulted |

## Secrets

Secrets come **only** from `SECRET_*` environment variables: `secret.admin-password` is read
from `SECRET_ADMIN_PASSWORD`, through `requireSecret` only.

- A `secret.*` key in a YAML file makes loading fail.
- A `-Dsecret.*` system property makes loading fail. The message names the property and the
  environment variable to use, never the value.
- **A secret passed with `-D` must be treated as exposed and rotated.** Surefire and Failsafe
  copy every system property into their XML reports before any test runs, so the value is
  recorded even though loading rejects it.
- In CI, map secrets through the workflow's `env:` block, never on the Maven command line (see
  the [`test-reporting` README](../test-reporting/README.md#supplying-secrets)).

**Limitations to know** (plan §7.8):

- **S2: protection depends on naming.** A credential under a key outside the `secret.`
  namespace, such as `db.password`, or an environment variable such as `AWS_SECRET_ACCESS_KEY`,
  is ordinary configuration. It is not in `secretValues()`, so reports do not mask it by value.
  Put every credential under `secret.*`.
- **S3: `reveal()` bypasses the mask.** Never use `Secret.reveal()` for logging, reporting or
  assertion messages: a failing assertion prints its message. Call `reveal()` only where the raw
  value is actually needed, such as an authentication request.
- **S4: secrets shorter than four characters** are not masked by value in reports, to avoid
  corrupting ordinary text; key-based masking still applies (`test-reporting`).

## How to run

```bash
./mvnw -pl platform/test-core verify          # tests and all quality gates for this module
./mvnw -pl platform/test-core test            # tests only
```

The tests (`TestConfigTest`) inject environment variables and system properties as maps, so they
never change the real process environment.

The module is coverage-gated by `platform/pom.xml`: line ≥ 80%, branch ≥ 70% (D3). It must keep
a `src/test/java` directory; without one the build fails in `validate` (R18). An *empty* test
directory still satisfies that guard, so code review remains the safeguard for test quality.
