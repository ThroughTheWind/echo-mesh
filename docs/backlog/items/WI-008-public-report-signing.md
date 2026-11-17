---
id: WI-008
title: Sign, encode and validate public reports
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [FR-4.1, FR-4.4, FR-4.5, FR-4.6, FR-5.2, FR-7.3, AC-10, ADR-0004]
epic: WI-006
children: []
---

## Proposal (rationale)

A public report is the unit everything else moves. It must carry nothing that identifies the sender, be authenticated with a rotating key, and be rejected cheaply when tampered with, replayed or expired.

Phase 1 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- `:core:model`: the report catalog of FR-4.1 with default lifetimes, coarsened position and time (FR-4.4), withdrawal (FR-4.5).
- `:core:crypto`: signing and verification with rotating keys as ADR-0004 and threat model §5.2 say, through Tink.
- Report id = hash of the canonical signed bytes (FR-5.2). Signed absolute creation time and expiry; clock-skew detection (FR-7.3).
- Sender-side rate limit (FR-4.6).

### Impacts

- `core/model`, `core/crypto`, the wire format from WI-003. Replaces the placeholder `ReportKind` and `Report`.

### Approach

- Verify-then-store: validation is one pure function from bytes to `Valid | Rejected(reason)`, used by the relay.

### Acceptance criteria / tests

- AC-10 (L): tampered, replayed-after-expiry and forged reports are rejected in unit tests and on the simulator.
- Two reports from the same phone across a key rotation cannot be linked by their bytes (test).
- The decoder fuzz target from WI-003 covers the report frame.

### Out of scope

- Group reports and votes: WI-019 and WI-020.

## Execution

## Review
