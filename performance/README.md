# performance

Load, stress and soak tests for the SDET Testing Playground (Phase 7).

| Directory | Contents |
|---|---|
| `jmeter/` | JMeter test plans (`.jmx`), executed through Maven |

Performance runs are opt-in and never part of the default build. Thresholds (latency
percentiles, error rate) are asserted so a regression fails the run.