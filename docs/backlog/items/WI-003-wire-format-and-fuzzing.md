---
id: WI-003
title: Decide the wire format and fuzz its decoder in CI
type: feature
status: review
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-17
related: [NFR-2, NFR-3, AC-12, ADR-0004, ADR-0005, F-001]
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
- F-001 narrows to the Noise test vectors, which land with the join (WI-017).

### Approach

- Start from the frame layouts in threat model §5.4 and the research size estimate (110–150 bytes signed).
- The compression gate reuses `ForbiddenDependenciesTask` with a second rules file scoped to `:core:*`.

### Acceptance criteria / tests

- AC-12: the fuzz target runs in CI with no crash, and a seeded crash case proves it can fail.
- Unit tests: every frame kind round-trips; truncated, oversized, wrong-version and trailing-byte inputs are rejected.
- The compression gate fails on a planted `org.tukaani:xz` (or similar) dependency and passes without it.

### Out of scope

- What a report means and how it is signed: WI-008. Group frame encryption: WI-018.

## Execution

- [ADR-0005](../../decisions/ADR-0005-wire-format.md) (accepted by Antoine on 2026-11-18) fixes version 1: a 3-byte header (version, kind, hop budget), then a fixed body per kind. Public report 114 bytes, vote 138, withdrawal 137, group frame 240. Positions in 10⁻⁴ degree, times in UTC minutes, lifetimes 1–180 minutes. The hop budget is outside the signed bytes and the message id. kotlinx.serialization is not used on the air.
- `core/mesh/.../wire/WireCodec.kt` decodes and encodes; `WireCodecTest` covers round trips of every kind, truncation at every length, trailing bytes, oversize, unknown version and kind, field ranges, which bytes are signed, and 20,000 seeded random inputs.
- `WireFuzzTest` (Jazzer 0.30.0, test classpath only): oracle is "never throws, and an accepted frame re-encodes to the bytes received". Run locally for 2 minutes: 79.6 million inputs, no finding. `WireFuzzSelfTest` plants a length-trusting bug; Jazzer found it in seconds. CI runs both on every pull request, and fails if the planted bug is not found.
- Compression gate: `gradle/forbidden-in-core.txt`, read by `checkForbiddenDependencies` for every `:core:*` module, and `checkCompressionImports` for `java.util.zip` and compression packages in `core/` source. Planting `org.tukaani:xz:1.10` in `:core:mesh` failed the first, and an `import java.util.zip.Inflater` failed the second; both pass without them.
- The fuzzing line of `.ai/rules/crypto.md` is now gated. F-001 narrows to the Noise vectors (WI-017).
- Android modules were not built locally (no Android SDK in the sandbox); CI builds them.

## Review
