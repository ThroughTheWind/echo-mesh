---
id: WI-024
title: Panic wipe in under two seconds
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [F11, FR-11.1, FR-11.2, FR-11.3, FR-11.4, FR-2.6, AC-17]
epic: WI-023
children: []
---

## Proposal (rationale)

If a phone is about to be seized, the person needs everything gone fast, without a gesture that fires by accident and without telling the mesh they wiped.

Phase 3 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Reachable in at most two actions from any screen; deliberate gesture (hold or slide) from the design system (FR-11.1).
- Destroy the storage key first, then delete data, groups, settings and optionally maps; stop the mesh (FR-11.2).
- Afterwards the app looks like a fresh install (FR-11.3); nothing is sent over the air (FR-11.4).

### Impacts

- `core/data` (key destruction from WI-010), `core/mesh`, `app`.

### Approach

- Time the wipe in an instrumented test on the slowest reference phone.

### Acceptance criteria / tests

- AC-17: the wipe completes in under 2 s, leaves a fresh install, and a radio capture shows nothing sent.

### Out of scope

- A duress PIN that wipes on entry: after 1.0.

## Execution

## Review
