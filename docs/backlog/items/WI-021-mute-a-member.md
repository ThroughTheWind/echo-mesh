---
id: WI-021
title: Mute a group member on this phone
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [FR-9.4, AC-24]
epic: WI-016
children: []
---

## Proposal (rationale)

An infiltrator or a member posting nonsense cannot be removed in the MVP. Muting hides them locally, so one person can protect their own view of the map.

Phase 2 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- A local action on a member hides their reports, votes and position on this phone only (FR-9.4).
- Stored encrypted with the group; cleared when the group is left or expires.

### Impacts

- `core/data`, `app` (roster).

### Approach

- Filter at display time; relaying is unchanged so the rest of the mesh is not affected.

### Acceptance criteria / tests

- AC-24: muting hides that member's reports, votes and position within one refresh.

### Out of scope

- Removing a member for everyone (re-keying): after 1.0.

## Execution

## Review
