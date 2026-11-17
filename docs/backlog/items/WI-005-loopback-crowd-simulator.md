---
id: WI-005
title: Grow the loopback adapter into a crowd simulator
type: feature
status: review
branch: claude/project-thread-67cn4e
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

- The relay logic the simulator exercises: WI-009.

## Execution

- `LoopbackNetwork` takes an optional `scope` and `clock`. Without a scope nothing changes (instant, lossless). With one, each link has a `LinkProfile` (latency range and loss rate), delivery waits its latency under virtual time, and every random draw comes from the seeded `random`, with neighbours visited in a fixed order so a run replays exactly. A frame in flight is lost if its link breaks or its receiver stops.
- `Crowd.grid` and `Crowd.randomGeometric` lay out crowds; `Trace` records every delivery, and `Coverage` gives first receipt, hop count, share of phones over time, time to a share and the curve.
- `HostilePhone` sends malformed, oversized (beyond the radio limit, as a modified stack could), replayed and flooding frames at a set rate; it bypasses the sender-side size check on purpose.
- Tests: `MeshTransportContract` passes on the instant network and again with 50–150 ms latency. A 200-phone crowd (100 m square, 15 m range, 200–800 ms and 10% loss per link, naive flooding as a stand-in relay) replays identically from seed 7, differs from seed 8, and reached 90% of phones in 2.6 s of virtual time over up to 10 hops. A hostile phone delivers 1,000 frames of each attack in 10 virtual seconds, 100 times an honest phone's rate.
- The naive flooding relay lives in the test only; the real relay is WI-009.

## Review
