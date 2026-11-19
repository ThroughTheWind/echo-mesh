---
id: WI-029
title: Phase 4 — Release 1.0
type: epic
status: planned
branch:
created: 2026-11-17
updated: 2026-11-19
related: [NFR-8, NFR-12, NFR-16, AC-26, AC-27, AC-29, AC-30, AC-31]
epic:
children: [WI-030, WI-031, WI-032, WI-033, WI-034, WI-035, WI-037, WI-038, WI-074]
---

## Proposal (rationale)

Phase 4 of the [roadmap](../../ROADMAP.md): an independent security review and its fixes, accessibility, a verified reproducible build, final docs, and 1.0.0, plus the extended spec's 1.0 additions: steering warnings (WI-034), low-density honesty (WI-035), three languages (WI-037) and release key custody (WI-038).

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
