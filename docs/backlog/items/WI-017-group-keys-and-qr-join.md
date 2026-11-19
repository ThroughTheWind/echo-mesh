---
id: WI-017
title: Create groups and join them in person by QR
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [F8, FR-8.1, FR-8.2, FR-8.3, FR-8.5, FR-8.6, FR-12.5, AC-13, AC-25, ADR-0004]
epic: WI-016
children: []
---

## Proposal (rationale)

Group keys must never cross the mesh. They travel in a Noise handshake over one direct hop after an in-person QR scan, and a photographed QR is worth at most one join.

Phase 2 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Create a group with a local name and lifetime (FR-8.1); keys generated in `:core:crypto`.
- QR with a one-time join key and token; open-source in-app scanner, no Play Services (FR-8.2).
- Noise handshake over one direct radio hop; every join visible to the group.
- Leave deletes keys; expiry deletes keys on every phone (FR-8.5, FR-8.6). QR screen uses `FLAG_SECURE`.

### Impacts

- `core/crypto`, `core/mesh` (direct-hop channel), `core/data`, `app`. A QR library must pass the dependency gate.

### Approach

- Follow ADR-0004 and threat model §5.5; tests for the handshake run on loopback with an attacker in the middle.

### Acceptance criteria / tests

- AC-25: scanning the same QR twice fails the second time, and the first join was shown to the group.
- The join handshake rejects a replayed or tampered message in tests.

### Out of scope

- Group report encryption: WI-018. Roster and join screens: WI-022.

## Execution

2026-11-19, the core half, built development-first while the phones are away. The layout is [ADR-0009](../../decisions/ADR-0009-group-frames-and-join.md) (accepted 2026-11-19).

- `:core:crypto`: `NoiseHandshake` (`Noise_NKpsk0_25519_ChaChaPoly_SHA256` on Tink and the JDK), checked byte for byte against the cacophony vectors, with tests for a wrong PSK, a wrong key, a changed byte, a replayed reply and reordered transport messages. `GroupEpochs`, `GroupCipher` and `MemberKey` generate and ratchet the group keys.
- `:core:mesh`: the join frame (kind `0x06`, 240 bytes, never relayed, fuzzed); `JoinOffer`, the QR text, base64url with one spelling; `JoinPayloads`, the fixed-size step plaintexts; `GroupChannel.invite` and `GroupChannel.join`, both sides of the join over one hop. A code answers the first valid step 1 only and is then destroyed; the member sends a member-added frame so the group sees every join; the newcomer gets the current epoch key and nothing older. Leave and expiry destroy the keys (`GroupBook`).
- Tests: `GroupMeshTest` on loopback joins by code, refuses a second scan of the same code (AC-25 in code), refuses a tampered token without burning the real code, and shows a phone replaying an overheard join lets nobody in. `JoinPayloadsTest` and the new `GroupFuzzTest` (a CI step of its own, 39 million local runs clean) cover the decoders.

Groups outlive a restart (2026-11-19). Each group is one sealed record, named by its random id and never by its name. The record holds the facts, the epoch keys held now, this phone's member key and the roster. It sits in the app's `SealedFiles` under the Keystore key (WI-010), behind the `SealedRecords` interface in `:core:mesh`, so the mesh never sees the key. A record is rewritten when its group is created, joined, gains a member or ratchets its keys, so a deleted epoch key leaves the record as well. Leaving or expiry deletes the record (FR-8.5, FR-8.6, FR-12.4). A record that cannot be opened, as while the phone is locked, is left alone rather than deleted. `GroupRecordTest` covers the round trip, the one encoding, damaged records and file names. A loopback test restarts a phone over the same store and reads the group's frames with the kept keys. Deleting a file unlinks it but does not erase flash, as WI-010 notes; only the panic wipe's key destruction is a real erase.

Still to do on this item: the QR screen with `FLAG_SECURE` and an open-source scanner in `app` (the screens themselves are WI-022, the library has to pass the dependency gate); AC-25 on two phones.

branch-merged-ok: the core half merges with PR #6 while the app half and the phone check stay open on this item.

## Review
