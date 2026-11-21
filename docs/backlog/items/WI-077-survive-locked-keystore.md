---
id: WI-077
title: Keep Echo running while the phone is locked and its storage key refuses
type: bug
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-20
updated: 2026-11-20
related: [WI-010, WI-024, WI-075, FR-3.3, FR-12.1, AC-5]
epic: WI-029
children: []
---

## Proposal (rationale)

On the phone check of 2026-11-20 Echo crashed on a locked Pixel 9a at 17:00 and the mesh stayed down for two hours, until the phone was unlocked and Echo opened ("Relaying stopped"). The group list's routine sweep rewrote a group record, and the sealing key, an Android Keystore key made `setUnlockedDeviceRequired(true)`, refused with `InvalidKeyException: Keystore operation failed`, caused by `KeyStoreException: Device locked`. Nothing caught it, so the process died. Any phone left locked in a pocket while its groups' keys ratchet loses the mesh: a release blocker for AC-5 and the field test (WI-028). The same refusal on a read looked like a tampered file, so settings first read while locked would have come back as defaults and been written over the real ones at the next change.

## Decision

`accepted` — 2026-11-20, Antoine's standing instruction to continue through each phase; the crash takes the mesh down in the situation it is built for, a phone in a pocket.

## Plan

### Requirements

- A sealing key the platform refuses for now never crashes Echo, on a write or a read.
- The key keeps `setUnlockedDeviceRequired(true)`: stored groups and settings stay unreadable on a locked phone (threat model §5.6). Nothing is written in the clear to get round it.
- A refused write is kept in memory and written once the phone is unlocked; a refused read is never taken for a missing or tampered file.

### Impacts

- `core/crypto`: `SealingUnavailableException`, thrown by `TinkSealingKey.seal` and `open` when the key refuses (`InvalidKeyException`, `ProviderException`). A failed tag still opens to null.
- `core/data`: `SettingsStore` keeps changes made while locked and replays them on the real file at `flush()`.
- `app`: `App` flushes settings on `ACTION_USER_PRESENT`; the group records adapter reads a refused record as one that cannot be opened, which `GroupChannel.restore` already leaves alone.
- `core/mesh`: none. `GroupChannel.save` already keeps a group in memory on an `IOException` and the next sweep writes it again; the new exception is one.

### Approach

- Translate the platform's refusal into one typed `IOException` at the crypto boundary, so every store's existing retry applies, instead of catching broad exceptions in each caller.

### Acceptance criteria / tests

- `SealingTest`: a key whose AEAD throws `InvalidKeyException` or `ProviderException` throws `SealingUnavailableException` on seal and open; a bad tag still opens to null.
- `SealedFilesTest`: a setting changed while locked applies at once and is on disk after unlock and flush; settings first read while locked are never overwritten with defaults, and the change is replayed on the kept ones; a locked read is unavailable, not missing.
- On the phones: a phone locked for an hour with a group whose key ratchets keeps relaying, with no crash in the crash buffer.

### Out of scope

- Groups that could not be read because Echo started while the phone was locked are loaded at the next start, not at unlock. Echo starts the mesh only from a tap with the app open, so this needs a process started from the lock screen (the discreet tile) followed by a mesh turn-on before a restart.
- Any other unhandled exception in the mesh's background loops: a crash handler that keeps the mesh up is its own item.

## Execution

2026-11-20.

- As planned. Found from the crash buffer of the Pixel's 17:00 crash (`GroupChannel.sweep` → `saveMoved` → `save` → `SealedFiles.write` → `TinkSealingKey.seal`).
- On the phones: the Pixel locked for over an hour on this build kept relaying with no crash. At 21:38, still locked, the sweep hit the same save; the Keystore refused, Echo kept the group in memory, and the record was written at 22:10, right after unlock.
- A cold start on a locked phone shows onboarding until unlock: F-010.
