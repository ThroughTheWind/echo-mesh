---
id: WI-045
title: Keep a jail-support card on the phone
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F23, US-31, FR-23.4, AC-36]
epic: WI-039
children: []
---

## Proposal (rationale)

A lawyer's number and a rights summary are needed exactly when the network is gone and the person may be detained.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- User-written number and notes, plus bundled or pack-provided rights text per country (FR-23.4).
- Stored encrypted, never sent over the air, removed by the panic wipe.
- Opens in two taps, only after device unlock, never on the lock screen.

### Impacts

- `core/data` (encrypted record), `app` (card screen), panic wipe (WI-024), bundled text per language (WI-037).

### Approach

- The one place free text exists, because it never leaves the phone; a test proves it is never encoded by the wire codec.

### Acceptance criteria / tests

- AC-36: opens offline in two taps after unlock and is gone after a panic wipe.
- A test that no wire frame can carry the card.

### Out of scope

- Rights text from event packs arrives with packs (WI-062).

## Execution

## Review
