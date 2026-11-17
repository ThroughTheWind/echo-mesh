---
id: WI-019
title: Confirm or dismiss a report, with votes that spread
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [F6, FR-6.1, FR-6.2, FR-6.3, FR-6.4, FR-6.5, AC-7, AC-8]
epic: WI-016
children: []
---

## Proposal (rationale)

Reports go stale and some are fake. Votes let the crowd correct the map without anyone being able to erase a pin early, and without public counts posing as proof.

Phase 2 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- One vote per report per phone; votes authenticated like reports and relayed the same way (FR-6.1, FR-6.2).
- A confirmation extends time left, capped at twice the default lifetime (FR-6.3).
- The "probably gone" rule exactly as FR-6.4 and threat model §5.7; never removes a pin.
- Copy says "N nearby phones confirmed", never "verified" (FR-6.5).

### Impacts

- `core/model`, `core/crypto`, `core/mesh`, `app` (report detail).

### Approach

- Votes are a frame kind of their own; public and group votes reuse WI-008 and WI-018 signing.

### Acceptance criteria / tests

- AC-7: a confirmation from B shows on A within 10 s and extends time left, never beyond twice the default.
- AC-8: dismissals mark a report "probably gone" without removing it.
- Unit tests for every branch of the FR-6.4 rule.

### Out of scope

- Muting a member's votes: WI-021.

## Execution

## Review
