---
id: WI-029
title: Phase 4 — Release 1.0
type: epic
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [NFR-8, NFR-12, NFR-16]
epic:
children: [WI-030, WI-031, WI-032, WI-033]
---

## Proposal (rationale)

Phase 4 of the [roadmap](../../ROADMAP.md): an independent security review and its fixes, accessibility, a verified reproducible build, final docs, and 1.0.0.

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Every child item is `done`.
- 1.0.0 is published; every spec acceptance criterion holds.

### Impacts

- 1.0 is the first version the project says people may rely on, within the threat model's stated limits.

### Approach

- The review (WI-030) gates the release (WI-033); WI-031 and WI-032 run alongside it.

### Acceptance criteria / tests

- All spec acceptance criteria pass, re-run on the release candidate.
- The 1.0.0 release page has the APK, its checksum and the review summary.

### Out of scope

- After-1.0 items (Wi-Fi Aware, duress PIN, re-keying, F-Droid listing) are proposed separately.

## Execution

## Review
