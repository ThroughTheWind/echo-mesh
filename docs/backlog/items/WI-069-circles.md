---
id: WI-069
title: Circles: long-lived groups that spawn event groups
type: feature
status: deferred
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F35, US-43, R4]
epic: WI-068
children: []
---

## Proposal (rationale)

Real affinity groups last months; meeting in person for every 7-day group may push them to other tools.

## Decision

`deferred` — 2026-11-17, Antoine, by accepting the [extended spec](../../SPEC-EXTENDED.md), which starts this only when its evidence condition is met. Revisit when: field use or user reports show the 7-day group limit pushing groups to other tools.

## Plan

### Requirements

- A long-lived circle secret, re-keyed on a schedule, deriving short-lived event groups without meeting again.

### Impacts

- New ratchet design, threat model section and ADR, outside review.

### Approach

- Plan in full when the trigger is met.

### Acceptance criteria / tests

- Written when it starts.

### Out of scope

- Everything until the trigger is met.

## Execution

## Review
