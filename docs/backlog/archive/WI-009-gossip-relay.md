---
id: WI-009
title: Relay reports by gossip with de-duplication, hop budget, rate limits and expiry
type: feature
status: done
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [FR-5.1, FR-5.2, FR-5.3, FR-7.1, NFR-1, NFR-3, AC-6, AC-9, AC-11, ADR-0008]
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

- The radio: WI-011. Votes refreshing expiry: WI-019.

## Execution

- **ADR-0008 (accepted by Antoine on 2026-11-18)** adds a summary frame to wire version 1: kind `0x05`, 240 bytes, a 16-byte random salt and a 1,768-bit Bloom filter over the held ids, 4 bits per id from SHA-256 of salt and id. Hop budget always 0, never stored or relayed. Added to version 1 rather than a version 2 because no phone runs version 1 yet. `WireCodec` decodes and encodes it, `WireFixtures` and the fuzz seeds include it, and the codec test checks a summary with any hop budget but 0 is rejected.
- **`Relay`** in `:core:mesh` (package `relay`), plain Kotlin against `MeshTransport`:
  - Verify, then store, then relay: every frame goes through `FrameValidator`; a valid one is stored, then passed to every other neighbour with one hop less. This phone's own frames leave with the budget the author set (a budget is the hops a frame may still take after the phone holding it), so a budget of 3 reaches four links away.
  - Anti-entropy: a summary on meeting and every 15 s (±50%) after; the neighbour pushes back what the filter lacks, withdrawals first, then newest, then most severe (`RelayConfig.severity`, a first guess for WI-028), within 16 KB, at most once per half period per link.
  - Budgets: per link 50 frames/s (burst 200) and 30 signature checks/s (burst 150), checked just before the signature; global 500 frames/s. Over budget, dropped, never queued.
  - `ReportStore`: in memory only, capped at 256, frames dropped at signed expiry plus 2 minutes of tolerated skew (threat model §5.2) by a sweep every 30 s, never relayed after. A withdrawal hides its report whichever arrives first.
  - `ClockCheck` is fed every first-seen message and every frame rejected for its time.
  - `reports` is a `StateFlow` of what to show; `Stats` counts what happened, for tests and the diagnostics item (WI-036).
- **Deviations.**
  - The digest in the BLE advertisement (FR-5.1) belongs to the radio and moves to WI-011; the relay only exchanges summaries over a link.
  - Eviction is not "fewest confirmations": votes are WI-019. It evicts from the link that brought the most frames, oldest first, so a flooder pushes out its own fakes; threat model §5.8 now says so.
- **Tests on the simulator** (`RelaySimulationTest`, seeded, virtual time):
  - AC-6: a report reaches a neighbour, disappears from both at expiry plus skew, and no copy is delivered after.
  - A phone that comes into range late gets the report from a summary.
  - A withdrawal spreads and hides the report on all nine phones of a grid.
  - The hop budget: in a line of ten phones, a budget of 3 reaches phones 0 to 4 only, even with summaries.
  - AC-9: 200 phones in a 100 m square, 15 m range, crowded BLE links (200–800 ms, 10% loss): 90% in 3.1 s of virtual time, 8 hops, 92% within 60 s (the hop budget of 7 bounds the rest).
  - AC-11: a hostile phone in range of 12 of 60 phones sends malformed, oversized, replayed and flooding traffic at 100 times the honest rate, the flood made of validly signed fakes. An honest report still reaches all 60 phones, 90% within 1.1 to 1.5 s.
- Unit tests: `SummaryTest` (every held id found, about 3.7% false positives at 256 ids, fresh salt each time), `ReportStoreTest` (fair eviction, expiry, withdrawals both ways, a foreign withdrawal hides nothing, token buckets).
- Fuzzing: 2 minutes locally, 64.8 million inputs, no finding, with the summary seed.
- The flooding run costs about 45 s of CPU locally, nearly all of it Ed25519 checks of the fakes across the crowd; the others take under 2 s.

## Review

Merged to `main` in PR #5 on 2026-11-19, at Antoine's request ("Accept and merge the prs"). Every acceptance criterion is met as the Execution section records.
