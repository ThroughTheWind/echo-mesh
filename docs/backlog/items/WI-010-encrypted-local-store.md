---
id: WI-010
title: Encrypt everything the app stores, with a Keystore key
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [FR-12.1, FR-12.2, FR-12.3, FR-12.4, NFR-5, AC-18]
epic: WI-006
children: []
---

## Proposal (rationale)

A seized phone must reveal as little as possible. The store holds only keys, groups and settings, all encrypted, and its key can be destroyed in one step for the panic wipe later.

Phase 1 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- `:core:data`: an encrypted DataStore (or file) whose key lives in the Android Keystore, created with unlocked-device-required (threat model §5.6).
- One API to destroy the key, used by the panic wipe (WI-024).
- Expired groups and keys are deleted, not hidden (FR-12.4).

### Impacts

- `core/data`, `core/crypto` (key wrapping through Tink). minSdk 31 makes unlocked-device-required available on every supported phone (ADR-0001).

### Approach

- Follow ADR-0004 and threat model §5.6; no home-made envelope format.

### Acceptance criteria / tests

- A test (Robolectric or instrumented) writes settings, reads them back, destroys the key, and finds the data unreadable.
- AC-18 (first half): a file-level look at a debug build's storage after use shows no plaintext keys, settings or group names.

### Out of scope

- The panic wipe flow and its timing: WI-024.

## Execution

## Review
