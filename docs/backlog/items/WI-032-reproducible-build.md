---
id: WI-032
title: Verify the build is reproducible and F-Droid ready
type: chore
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [NFR-16, ADR-0003]
epic: WI-029
children: []
---

## Proposal (rationale)

Anyone should be able to rebuild the APK and get the same bytes, which is what makes the "no tracker" claim checkable and what F-Droid needs.

Phase 4 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Two independent builds of the same tag produce identical unsigned APKs (or the differences are explained and removed).
- F-Droid metadata drafted; inclusion itself is after 1.0.

### Impacts

- Build files, CI (a reproducibility job).

### Approach

- Follow F-Droid's reproducible-builds guidance.

### Acceptance criteria / tests

- A CI job builds twice and compares, and fails on a difference.

### Out of scope

- Submitting to F-Droid: after 1.0.

## Execution

## Review
