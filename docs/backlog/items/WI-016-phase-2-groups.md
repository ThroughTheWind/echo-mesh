---
id: WI-016
title: Phase 2 — Groups: ship 0.2.0
type: epic
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [AC-7, AC-8, AC-13, AC-14, AC-15, AC-16, AC-24, AC-25]
epic:
children: [WI-017, WI-018, WI-019, WI-020, WI-021, WI-022]
---

## Proposal (rationale)

Phase 2 of the [roadmap](../../ROADMAP.md): affinity groups joined in person, encrypted group reports relayed by everyone, confirm and dismiss, opt-in position sharing, and muting a member.

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Every child item is `done`.
- 0.2.0 is published.

### Impacts

- The group protocol is the most security-sensitive code after the relay; it follows ADR-0004 exactly.

### Approach

- WI-017 (keys and join) first; WI-018 to WI-021 follow; WI-022 (screens) tracks them.

### Acceptance criteria / tests

- AC-7, AC-8, AC-13 to AC-16, AC-24 and AC-25 pass on the reference phones or the simulator as marked.
- The 0.2.0 release page has the APK and its checksum.

### Out of scope

- Removing a member by re-keying: after 1.0 (spec §3.2).

## Execution

## Review
