---
id: WI-043
title: Vibrate on danger reported nearby
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F22, US-29, FR-22.1, FR-22.2, FR-22.3, FR-22.4, FR-22.5, NFR-18, AC-34]
epic: WI-039
children: []
---

## Proposal (rationale)

Phones should stay in pockets. A distinct vibration for tear gas or a kettle reported close by gives the information without looking at a lit screen.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Opt-in alerts per type and radius (defaults from FR-22.1).
- Group reports only by default; public opt-in with a warning (FR-22.2).
- No text, type or map on the lock screen (FR-22.3); one alert per type per 2 minutes (FR-22.4).
- Location in the foreground service only; off with a reason without permission (FR-22.5).
- NFR-18: alert within 5 s of receipt.

### Impacts

- `app` (settings, notification), relay service, location use while the mesh is on, threat model §7 copy.

### Approach

- Evaluate distance on receipt against the last known own position; never store positions.

### Acceptance criteria / tests

- AC-34 on a locked reference phone.
- Test that the notification carries no type or text.

### Out of scope

- Trusted-role alerts arrive with roles (WI-058).

## Execution

## Review
