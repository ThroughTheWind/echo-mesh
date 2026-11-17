---
id: WI-026
title: Final onboarding and "What this app protects" copy, traced to the threat model
type: docs
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [F1, FR-1.1, FR-1.3, FR-3.4a, NFR-14]
epic: WI-023
children: []
---

## Proposal (rationale)

The app must never claim more safety than it delivers. Every sentence that makes a safety claim must trace to a row of threat model §7, so the security review can check it.

Phase 3 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Final copy for onboarding and the settings page, each claim citing its §7 row.
- The residual risk of FR-3.4a (the constant service UUID) is stated.
- A check (test or gate) that every claim string has a §7 reference.

### Impacts

- `app` strings, `docs/threat-model.md` §7 if a row is missing.

### Approach

- Antoine reviews the wording; the design thread's onboarding components carry it.

### Acceptance criteria / tests

- Every safety claim in the UI maps to a §7 row, and the check fails when one does not.

### Out of scope

- Translations: not planned (NFR-17).

## Execution

## Review
