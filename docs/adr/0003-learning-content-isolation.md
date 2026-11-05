# ADR-0003: Keep learning content in the repository, isolated under `learning/`

- **Status:** Accepted
- **Date:** 2026-11-05

## Context

The repository serves two purposes: a production-grade automation portfolio and a record of
study (Java fundamentals, design patterns, interview preparation). Mixing the two would
weaken the production code's clarity; splitting them into separate repositories would
fragment the portfolio.

## Decision

Keep learning content in this repository under a dedicated top-level `learning/` directory:

- `foundation/*` moved to `learning/` (`java-core`, `collections-streams`,
  `oops-design-patterns`).
- `interview-preparation/` moved to `learning/interview-preparation/`.

Isolation rules:

- No production module (`platform`, `frameworks`, `test-suites`, `sut`) may depend on
  `learning/`.
- Java practice code is built only through an opt-in Maven profile, never by default.
- The same formatting and static analysis rules apply, so examples model good practice.

## Consequences

- One repository tells the full story; production modules stay uncluttered.
- The opt-in profile must be added when the first Java code lands in `learning/`.

## Alternatives considered

- **Separate knowledge-base repository:** cleanest separation, rejected to keep everything in
  one place.
- **Under `docs/`:** rejected because `docs/` is for documentation, not source code.