---
id: WI-001
title: Phase 0 — Foundations: finish what every later phase builds on
type: epic
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [AC-20, AC-21, AC-22, AC-23, ADR-0001, ADR-0002, ADR-0003, ADR-0004]
epic:
children: [WI-002, WI-003, WI-004, WI-005]
---

## Proposal (rationale)

Phase 0 of the [roadmap](../../ROADMAP.md). The scaffold (build, rungs, CI, the boundary, dependency and manifest gates, ADR-0001 to 0003, the `MeshTransport` contract and `:adapters:loopback`) merged in PR #1 before ids existed, so it has no item. What remains is the decisions and test harness the protocol code needs before it is written: the crypto ADR accepted, the wire format fixed and fuzzed, the map path decided, and a loopback simulator good enough to test a crowd.

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Every child item is `done`.
- Exit criteria from the roadmap: AC-20 to AC-23 hold on `main`.

### Impacts

- Nothing user-visible. Every Phase 1 item depends on at least one child.

### Approach

- Children run in parallel; none depends on another except that WI-003 builds on ADR-0004 (WI-002).

### Acceptance criteria / tests

- AC-20, AC-21 and AC-22 (build part) pass in CI on `main` (they already do since PR #1; re-check at close).
- AC-23: `docs/threat-model.md` §6 answers every attack in both Bridgefy papers (re-check at close).
- WI-002 to WI-005 are `done`.

### Out of scope

- The signed release on tag (rest of AC-22) is proven in Phase 1 by WI-015, when there is something to release.

## Execution

## Review
