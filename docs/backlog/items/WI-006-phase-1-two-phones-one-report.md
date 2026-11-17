---
id: WI-006
title: Phase 1 — Two phones, one report: ship 0.1.0
type: epic
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-6a, AC-9, AC-10, AC-11, AC-12]
epic:
children: [WI-007, WI-008, WI-009, WI-010, WI-011, WI-012, WI-013, WI-014, WI-015]
---

## Proposal (rationale)

Phase 1 of the [roadmap](../../ROADMAP.md): a signed, expiring public report crosses from one reference phone to another over BLE and shows on an offline map. Basics first: no groups, no second transport.

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Every child item is `done`.
- 0.1.0 is published on GitHub Releases by the release workflow.

### Impacts

- First user-visible release. Pre-alpha: the release notes say plainly what it does not protect yet.

### Approach

- The spike (WI-007) goes first: it settles the radio facts WI-011 depends on. WI-008, WI-009 and WI-012 can run in parallel once Phase 0 is done.

### Acceptance criteria / tests

- AC-1 to AC-6a pass on every pair of reference phones, the OPPO included, recorded in the children.
- AC-9 to AC-12 pass in CI.
- The 0.1.0 release page has the APK and its checksum.

### Out of scope

- Groups, votes and position sharing: Phase 2 (WI-016). Panic wipe and discreet mode: Phase 3 (WI-023).

## Execution

## Review
