---
id: WI-042
title: Keep one rally point per group
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F21, US-28, FR-21.1, FR-21.2, FR-21.3, AC-33]
epic: WI-039
children: []
---

## Proposal (rationale)

Affinity groups already agree a meeting point if separated. Making it a shared pin keeps it in sync and gives "Regroup at the rally point" somewhere to point.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- One pin plus optional time per group, set by any member, latest wins, setter shown (FR-21.1, FR-21.2).
- Held in memory for the group's life and re-shared to new joiners.
- Distinct map style; the regroup phrase points to it (FR-21.3).

### Impacts

- `core/model`, group payload (reuses WI-041's mechanism), `app` map layer, design component.

### Approach

- A group payload kind re-sent on join and periodically while the group is live.

### Acceptance criteria / tests

- AC-33: a rally point set on A shows on B and C, and a new joiner receives it.

### Out of scope

- Multiple rally points or schedules; nothing deferred.

## Execution

## Review
