---
id: WI-027
title: Audit storage and radio for leaks
type: chore
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [AC-18, AC-19, NFR-4, NFR-5]
epic: WI-023
children: []
---

## Proposal (rationale)

Two acceptance criteria are checks of the artifact, not the code: what is on disk after use, and what is on the air over half an hour.

Phase 3 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- AC-18: inspect a debug copy of app storage after a realistic session; no plaintext reports, positions, keys or group names.
- AC-19: a 30-minute BLE capture (open-source sniffer) shows no app token or address surviving a 10-minute rotation.
- Write the procedure down so it can be repeated before each release.

### Impacts

- A procedure under `docs/`; any leak found becomes its own item.

### Approach

- Run on two reference phones, the OPPO included.

### Acceptance criteria / tests

- AC-18 and AC-19 pass, with the evidence (file listing, capture summary) recorded in Execution.

### Out of scope

- Fixing leaks found: new items, not this one.

## Execution

## Review
