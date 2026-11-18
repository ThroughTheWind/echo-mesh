---
id: WI-071
title: Shade areas to avoid from trusted reports
type: feature
status: deferred
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F37, E7]
epic: WI-068
children: []
---

## Proposal (rationale)

Shading reads faster than pins in a dense area, but looks more authoritative, so it uses group and trusted-role reports only and is never a route.

## Decision

`deferred` — 2026-11-17, Antoine, by accepting the [extended spec](../../SPEC-EXTENDED.md), which starts this only when its evidence condition is met. Revisit when: field tests show pins become unreadable in dense areas, after roles (WI-058) ship.

## Plan

### Requirements

- Shading from group and trusted-role reports only; labelled "not a route".

### Impacts

- `core/map`, `app`, design.

### Approach

- Plan in full when the trigger is met.

### Acceptance criteria / tests

- Written when it starts.

### Out of scope

- Everything until the trigger is met.

## Execution

## Review
