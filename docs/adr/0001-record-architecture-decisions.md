# ADR-0001: Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-11-05

## Context

This repository is built incrementally over many phases. Without a written record, the
reasons behind structural and tooling choices are lost, and later changes risk silently
reversing them.

## Decision

Record every significant decision as a numbered Markdown ADR in `docs/adr/`, using
[`0000-template.md`](0000-template.md). A decision is significant if it affects module
structure, adds or replaces a technology, changes a quality gate, or would be expensive to
reverse.

ADRs are immutable once accepted; a changed decision is recorded as a new ADR that
supersedes the old one.

## Consequences

- Reviewers can understand why the repository looks the way it does.
- A small amount of writing accompanies each significant change.