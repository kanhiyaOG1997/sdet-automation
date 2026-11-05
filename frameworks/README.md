# frameworks

Reusable automation frameworks. Each is a library: it contains no tests against the system
under test, only unit tests for its own behaviour.

| Module | Technology | Phase |
|---|---|---|
| `api-framework` | RestAssured | 2 |
| `selenium-framework` | Selenium 4 | 4 |
| `playwright-framework` | Playwright Java | 4 |
| `appium-framework` | Appium | 8 |

Rules:

- Frameworks depend on `platform/` modules only — never on each other.
- Driver and browser instances are thread-confined so suites can run in parallel.