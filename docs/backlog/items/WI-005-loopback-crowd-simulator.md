---
id: WI-005
title: Grow the loopback adapter into a crowd simulator
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [FR-14.3, NFR-1, NFR-15, AC-9, AC-11]
epic: WI-001
children: []
---

## Proposal (rationale)

AC-9 and AC-11 are proven on the JVM, not in a street: 200 phones, 5 hops, a flooding peer. Today `LoopbackNetwork` only links phones by hand, with instant, lossless delivery.

Phase 0 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Topologies: generated crowd layouts (grid, random geometric graph at a given density) as well as hand links.
- Per-link latency and loss, and a virtual clock so a 60-second spread runs in milliseconds and deterministically from a seed.
- Hostile phones: send malformed, oversized, replayed and flooding frames at a set rate.
- Metrics: per-report coverage over time and hop count, readable from tests.

### Impacts

- `adapters/loopback` only; tests in `:core:mesh` use it. No production code changes.

### Approach

- Drive delivery from `kotlinx-coroutines-test` virtual time; keep `MeshTransportContract` passing.

### Acceptance criteria / tests

- `MeshTransportContract` still passes on the simulator.
- A seeded 200-phone run is reproducible: the same seed gives the same coverage curve.
- A test shows a hostile phone sending each bad frame kind at 100× the normal rate (the harness for AC-11).

### Out of scope

- The relay logic the simulator exercises: WI-010.

## Execution

## Review
