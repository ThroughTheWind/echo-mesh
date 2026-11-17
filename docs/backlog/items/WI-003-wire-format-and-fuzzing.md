---
id: WI-003
title: Decide the wire format and fuzz its decoder in CI
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [NFR-2, NFR-3, AC-12, ADR-0004, F-001]
epic: WI-001
children: []
---

## Proposal (rationale)

Every frame a phone receives is hostile until verified, and Bridgefy fell to a single crafted message. The format must be versioned, fixed-layout and size-checked from the first byte, and its decoder fuzzed before any other code trusts it (threat model §5.4).

Phase 0 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- An ADR fixes the wire format: version byte first, fixed layout, no compression, the size cap per frame kind, which bytes are signed, and why kotlinx.serialization is or is not used on the air.
- A decoder in `:core:mesh` (or a `:core:wire` module if the ADR says so) that checks size before parsing, never allocates from a length read off the air, and returns a typed rejection instead of throwing.
- A fuzz target (Jazzer or equivalent, GPL-compatible, no network) run in CI on every PR for a fixed time budget.
- A gate that fails if a compression library is on any `:core:*` runtime classpath (F-001).

### Impacts

- New ADR, `core/mesh`, `.github/workflows/ci.yml`, `build-logic` (compression gate), `.ai/rules/crypto.md` (the fuzzing line becomes `gated`).
- F-001 closes when this item and WI-009 land.

### Approach

- Start from the frame layouts in threat model §5.4 and the research size estimate (110–150 bytes signed).
- The compression gate reuses `ForbiddenDependenciesTask` with a second rules file scoped to `:core:*`.

### Acceptance criteria / tests

- AC-12: the fuzz target runs in CI with no crash, and a seeded crash case proves it can fail.
- Unit tests: every frame kind round-trips; truncated, oversized, wrong-version and trailing-byte inputs are rejected.
- The compression gate fails on a planted `org.tukaani:xz` (or similar) dependency and passes without it.

### Out of scope

- What a report means and how it is signed: WI-009. Group frame encryption: WI-019.

## Execution

## Review
