---
id: WI-020
title: Share position with a group, opt-in and short-lived
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [F10, FR-10.1, FR-10.2, FR-10.3, FR-10.4, AC-15]
epic: WI-016
children: []
---

## Proposal (rationale)

Knowing where your group is matters at a protest, and it is also the most dangerous data the app handles. It must be off by default every time, encrypted to the group, and never stored.

Phase 2 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Per-group opt-in that turns itself off with the mesh, on app kill, or after the chosen duration (FR-10.1).
- Encrypted to the group, at a modest interval, each position expiring after a few minutes (FR-10.2).
- Received positions in memory only; latest per member with its age (FR-10.3).
- A persistent indicator with one-tap stop (FR-10.4).

### Impacts

- `core/mesh`, `core/crypto`, `app`. Reuses group frames from WI-018.

### Approach

- Interval and lifetime as constants to tune in the field test (WI-028).

### Acceptance criteria / tests

- AC-15: B shares, A sees it with its age; B stops with one tap and A's view goes stale then disappears.
- A test proves no position reaches storage (with WI-010's store inspected).

### Out of scope

- Background location: never (NFR-10). Tuning the interval: WI-028.

## Execution

## Review
