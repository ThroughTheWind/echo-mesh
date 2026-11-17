---
id: WI-002
title: Review and accept the crypto design (ADR-0004)
type: docs
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-17
related: [ADR-0004, SPEC-A3, SPEC-A4, NFR-8]
epic: WI-001
children: []
---

## Proposal (rationale)

ADR-0004 (signed frames, a ratcheted group key, a Noise join, Tink) is `proposed`. Every report, relay and group item codes against it, so it must be settled first; changing it after code exists means rewriting the security-critical part twice.

Phase 0 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- ADR-0004 is read against `docs/threat-model.md` §5 and the Bridgefy checklist (§6), and every open question in threat model §9 that blocks Phase 1 or 2 has an answer.
- Antoine accepts it, or it is revised and then accepted. Status becomes `accepted` with the date.
- Tink is added to `gradle/libs.versions.toml` (`tink` for JVM modules), and its package to `CRYPTO_PACKAGES` in `build-logic` if it is missing.

### Impacts

- `docs/decisions/ADR-0004-*.md`, spec assumptions A3 and A4, `gradle/libs.versions.toml`.
- Records are immutable once accepted: later changes supersede.

### Approach

- A docs-only PR. Questions that cannot be settled on paper (for example a Noise pattern detail) become explicit items rather than open text in the ADR.

### Acceptance criteria / tests

- ADR-0004 has `status: accepted` and passes `adr-sections-present`.
- Threat model §9 has no open question that a Phase 1 item depends on.
- `checkForbiddenDependencies` passes with Tink on a JVM module classpath (Tink is allowed, `com.google.android.gms` is not).

### Out of scope

- Writing crypto code: WI-008 (public reports) and WI-018 (groups).
- The outside security review: WI-030.

## Execution

- Read ADR-0004 against threat model §5 and the Bridgefy checklist (§6): every construction it names is the one §5.2, §5.3 and §5.5 rely on, and §6 has no row it leaves open. Its wire rules now point to ADR-0005.
- Threat model §9: no open question blocks Phase 1 or 2. Launcher disguise and muting are settled in the spec (FR-13.2, FR-9.4); the app lock is v1.1; join reliability and rotation periods are field-test tuning (WI-028).
- Tink 1.24.0 is in the catalog and on `:core:crypto`. Deviation from the plan, recorded in ADR-0004 while it is still proposed: `tink-android` is used on the JVM too, because the JVM `tink` artifact pulls in `google-http-client` and native Conscrypt. `gradle/forbidden-dependencies.txt` now refuses `com.google.crypto.tink:tink`, `com.google.http-client` and the Conscrypt uber jar; planting `tink` on `:core:crypto` failed the gate.
- `TinkPrimitivesTest` proves on the JVM the four primitives the design uses: Ed25519, XChaCha20-Poly1305, X25519, and HKDF-SHA256 against RFC 5869 test case 1.
- `com.google.crypto.tink` was already in `CRYPTO_PACKAGES`.
- **Waiting on Antoine:** accept ADR-0004 (or ask for changes). Status stays `in_progress` until then.

## Review
