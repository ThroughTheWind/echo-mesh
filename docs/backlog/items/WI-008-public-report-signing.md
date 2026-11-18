---
id: WI-008
title: Sign, encode and validate public reports
type: feature
status: review
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-18
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

- Group reports and votes: WI-018 and WI-019.

## Execution

- **Module graph.** `:core:mesh` now depends on `:core:model` (`api`: frames carry the report catalog) and on `:core:crypto` (`implementation`, so adapters, which see `:core:mesh` through `api`, never get a crypto API on their compile classpath). `:core:crypto` and `:core:model` depend on nothing of ours.
- **`:core:model`.** `ReportType` is the FR-4.1 catalog in its order, with its default lifetimes; it moved here from the wire package, and its `code` is the catalog position ADR-0005 puts on the air. `Report` replaces the placeholder `ReportKind` with that type and a `lifetime`.
- **`:core:crypto`.** `OneTimeSigningKey` (a fresh Ed25519 key per report, kept in memory, its seed zeroed after use, never printed), `Signatures.verify` (any malformed input is `false`, never an exception) and `Hashes.sha256`, all through Tink and `java.security`. `SignaturesTest` adds the FIPS 180-2 `abc` vector for SHA-256.
- **`:core:mesh`, package `report`.**
  - `FrameValidator.validate(bytes, nowMinute, seen)` is the one pure check the relay calls, in threat model §5.4 order: decoder (size, version, layout), expiry (more than 5 minutes in the future, or past creation + lifetime + 2 minutes), de-duplication through `seen`, then the Ed25519 signature over the decoder's `signedBytes`. It returns `Valid(id, frame, received)` or `Rejected(reason)`. The id is SHA-256 of the received bytes without the hop budget. `withdraws(withdrawal, report, id)` is the meaning check for FR-4.5: same id, same one-time key. Group frames are `UNSUPPORTED_KIND` until WI-018; public votes verify like reports and get their meaning in WI-019.
  - `ReportAuthor` signs this phone's reports: the creation time is cut to the minute and the position to the wire's 10⁻⁴ degree grid before signing; each report gets a new key, held until the report expires to sign a withdrawal; at most one report per type per minute (FR-4.6). A withdrawal lives until the report would have expired.
  - `ClockCheck` answers FR-7.3 from the signed creation times of first-seen messages: "behind" when the median is more than 5 minutes in this phone's future, "ahead" when even the freshest quarter is more than 5 minutes old, nothing with fewer than 8 in 10 minutes. Deviation from threat model §5.2's "median of recent peers": peers have no identity and a phone receives old reports with new ones, so "ahead" reads the freshest quarter; §5.2 now says so. The relay (WI-009) feeds it.
- **Tests.** `FrameValidatorTest` (every single-bit flip outside the hop budget is refused; the hop budget can change without changing the id; expiry and grace; the future limit; duplicates are refused before the signature is checked; swapped keys, swapped signatures and an unsigned frame are refused; withdrawals match only their report). `ReportAuthorTest` (coarsening, the rate limit, withdrawal, key forgotten at expiry, ranges, the poles and the antimeridian; two reports of the same content from one phone differ only in key, signature and creation minute). `ClockCheckTest`. On the simulator, `ReportValidationSimulationTest`: an honest phone and a hostile one send to the same receiver; the tampered, replayed-after-expiry and forged frames are refused and the honest one is valid.
- **Fuzzing.** The decoder did not change beyond an import; `WireFuzzTest` already covers the report frame and its seeds include one.
- Run locally: `:core:model`, `:core:crypto`, `:core:mesh` and `:adapters:loopback` tests (92 tests, 0 failures), the transport, crypto, compression and dependency gates, and ktlint on changed files.

## Review
