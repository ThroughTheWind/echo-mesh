---
id: WI-028
title: Field-test in a crowd and tune lifetimes, rates and battery
type: spike
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [NFR-1, NFR-9, FR-4.1, FR-10.2]
epic: WI-023
children: []
---

## Proposal (rationale)

Lifetimes, rates, gossip budgets and the position interval are guesses until tried among many phones. The roadmap asks for a real crowd or the closest safe equivalent.

Phase 3 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Plan a test with as many phones as available (a real gathering or a staged one), all on the 0.3.0 candidate.
- Measure propagation (NFR-1 field target), battery per hour with the mesh on (NFR-9), and report lifetimes in use.
- Record changes to constants with the evidence.

### Impacts

- Constants in `core/model` and `core/mesh`; the spec's proposed values updated.

### Approach

- Antoine organises; results go in Execution.

### Acceptance criteria / tests

- A results table for propagation and battery against NFR-1 and NFR-9.
- Every tuned constant changed in one PR citing this item.

### Out of scope

- A second transport if BLE misses NFR-1: a new item, decided from these results.

## Execution

## Review
