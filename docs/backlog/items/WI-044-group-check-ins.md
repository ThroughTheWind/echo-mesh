---
id: WI-044
title: Show who missed a check-in
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F23, US-30, FR-23.1, FR-23.2, FR-23.3, FR-23.5, AC-35]
epic: WI-039
children: []
---

## Proposal (rationale)

Jail support starts with knowing who is missing. A timed "I'm OK" makes silence visible to the group without the missing phone ever broadcasting anything.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Start a check-in with interval and end time; reminder vibration; one tap sends "I'm OK" (FR-23.1).
- Overdue shows in roster and feed; nothing sent on the missing member's behalf (FR-23.2).
- Copy says a miss can mean a dead battery or no peers (FR-23.3).
- Ending sends "I'm safe" (FR-23.5).

### Impacts

- Group payload (check-in start with interval, reuses WI-041 phrases), roster UI, copy.

### Approach

- Members compute overdue locally from the announced interval; no new wire kind beyond the check-in start.

### Acceptance criteria / tests

- AC-35: a missed check-in appears on the others; the missing phone sends nothing.

### Out of scope

- The jail-support card (WI-045).

## Execution

## Review
