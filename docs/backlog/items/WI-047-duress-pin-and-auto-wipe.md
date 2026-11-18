---
id: WI-047
title: Add a duress PIN and auto-wipe
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F24, FR-24.2, FR-24.3, FR-24.4, AC-37]
epic: WI-039
children: []
---

## Proposal (rationale)

Under coercion, a second PIN that silently wipes is the last line; auto-wipe covers a phone that is never unlocked again. Both carry legal and data-loss risks the user must see.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Duress PIN opens a fresh-looking app after a silent panic wipe, with the setup warning about evidence laws and open-source discoverability (FR-24.2).
- Auto-wipe after a chosen time without unlock, run by a background job (FR-24.3).
- Both opt-in, off by default (FR-24.4).

### Impacts

- `app`, panic wipe (WI-024), background scheduling, threat model §11 (WI-040).

### Approach

- Reuse the panic wipe; the duress path must be indistinguishable in timing from a normal unlock.

### Acceptance criteria / tests

- AC-37 (duress part): the duress PIN opens a fresh app with no readable data.
- Auto-wipe fires on a test clock.

### Out of scope

- Nothing deferred.

## Execution

## Review
