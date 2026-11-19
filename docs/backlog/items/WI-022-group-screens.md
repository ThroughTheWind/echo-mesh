---
id: WI-022
title: Build the group screens: create, join, roster, verified style and filters
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [F8, F9, FR-4.3, FR-8.4, FR-8.7, FR-9.2, FR-9.3]
epic: WI-016
children: []
---

## Proposal (rationale)

Groups are only safe if their screens make the risks plain: who can read, what a QR gives away, and which pins are verified.

Phase 2 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Create, join (scanner), show QR with its warning (FR-8.3), leave; nickname per group (FR-8.4).
- Roster of members seen recently with last-seen age, up to 5 groups (FR-8.7).
- Audience choice on the send button (FR-4.3); verified style and group colour (FR-9.2); filter (FR-9.3).
- Built from the design system's group components.
- Carried from WI-020 (2026-11-19): the member sheet with distance and "Show on map" (screens/GroupRoster), and the sharing line in the mesh notification while the app is in the background (patterns/PositionSharing, step 2).

### Impacts

- `app`, `core/designsystem`.

### Approach

- Screens land with the feature they expose; this item closes when all exist.

### Acceptance criteria / tests

- AC-13 end to end through the UI on two reference phones.
- The QR screen blocks screenshots (`FLAG_SECURE`).

### Out of scope

- Final copy review: WI-026.

## Execution

## Review
