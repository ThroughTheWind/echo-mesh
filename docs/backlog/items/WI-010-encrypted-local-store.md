---
id: WI-010
title: Encrypt everything the app stores, with a Keystore key
type: feature
status: review
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-18
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

- **`:core:crypto`**: `Sealer` and `SealingKey` (seal, open, destroy), over a Tink AEAD so the ciphertext format is Tink's. `Sealers.ephemeral()` is an in-memory XChaCha20-Poly1305 key for tests. `open` returns null for a wrong key, a wrong context, a changed byte or a destroyed key, never an exception.
- **`:core:crypto:keystore`** (new Android library, under `core/crypto/` so `checkCryptoBoundaries` allows its Keystore imports): `KeystoreSealingKey.getOrCreate(alias)` makes an AES-256-GCM key in the Android Keystore with `setUnlockedDeviceRequired(true)`, StrongBox first and the TEE if the phone has no StrongBox, and seals through Tink's `AndroidKeystoreAesGcm`. `destroy` deletes the Keystore entry. No Tink keyset is stored, so there is no keyset file to fall back to cleartext.
- **`:core:data`**: `SealedFiles` (one sealed file per name, the name bound as context so files cannot be swapped, written through a temporary file and one rename; `destroy()` destroys the key first, then deletes the files) and `SettingsStore` (Phase 1 settings as JSON in one sealed file, with a `StateFlow`). Settings hold only `onboardingDone` and `lastAudienceIsPublic` for now; items add fields as they need them.
- **Deviation:** `:core:data` is now a plain JVM library, not the Android library ADR-0001 lists. The app hands it a directory and the Keystore key, so every test runs on the JVM without Robolectric, which has no Android Keystore anyway. ADR-0001's reason for Android (the Keystore) is met by `:core:crypto:keystore`.
- **Tests:** `SealedFilesTest` writes settings, restarts, reads them back, finds no plaintext on disk, destroys the key and finds even pre-wipe copies of the files unreadable; swapped and changed files do not open; names cannot leave the directory. `SealingTest` covers the same at the key level.
- **Not done here:** the Keystore key itself runs only on a phone. It compiles in CI; it is exercised on the reference phones with the app's first screens (WI-013), and AC-18's file-level look at a debug build's storage needs a phone too, so that acceptance line stays open until then.
- Deleting one file (FR-12.4) unlinks it but does not erase flash; only destroying the key is a real erase. Expired group keys (Phase 2) are deleted per file; the threat model already counts on the crypto-erase for a wipe.

## Review
