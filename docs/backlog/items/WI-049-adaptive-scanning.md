---
id: WI-049
title: Scan less when the crowd is dense
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F26, US-34, FR-26.1, FR-26.2, FR-26.3, NFR-22, AC-39]
epic: WI-039
children: []
---

## Proposal (rationale)

At 5% per hour a six-hour protest costs a third of the battery. Dense crowds need less scanning, not more.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Duty cycle adapts to density and change (FR-26.1), policy in `core/mesh` (FR-26.2).
- At least 30% less battery per hour at high density with no more than 25% longer median delay (FR-26.3); NFR-22.

### Impacts

- `core/mesh` (policy), `adapters/ble` (applies it), simulator scenarios, field diagnostics (WI-036).

### Approach

- Tune in the crowd simulator first, then confirm with field diagnostics on the reference phones.

### Acceptance criteria / tests

- AC-39 in the simulator and on the reference phones.

### Out of scope

- Wi-Fi Aware duty cycles (WI-064).

## Execution

## Review
