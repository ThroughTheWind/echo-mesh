---
id: WI-009
title: Relay reports by gossip with de-duplication, hop budget, rate limits and expiry
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [FR-5.1, FR-5.2, FR-5.3, FR-7.1, NFR-1, NFR-3, AC-6, AC-9, AC-11]
epic: WI-006
children: []
---

## Proposal (rationale)

Reports must reach the crowd without one phone flooding it, and without the relay path naming any phone. This is the code most exposed to hostile peers.

Phase 1 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Epidemic gossip with anti-entropy (FR-5.1): digest advertisement, set summary on connection, pull newest and most severe first within a per-connection byte budget.
- De-duplication on report id; hop budget decremented per relay; drop at expiry (FR-7.1).
- Per-connection and global rate limits; bounded memory with eviction by expiry, then fewest confirmations (FR-5.3).
- In memory only: nothing about reports touches disk (FR-12.2).

### Impacts

- `core/mesh`. Consumes WI-005 for tests and WI-008 for validation.

### Approach

- Plain Kotlin against `MeshTransport`; every test runs on the loopback simulator.

### Acceptance criteria / tests

- AC-9 (L): a report crosses 5 hops in a 200-phone simulation; 90% coverage in under 60 s of virtual time.
- AC-11 (L): a flooding peer at 100× normal rate does not stop honest reports reaching other peers.
- AC-6: an expired report is dropped everywhere and never relayed again (simulator test).

### Out of scope

- The radio: WI-011. Votes refreshing expiry: WI-020.

## Execution

## Review
