---
id: ADR-0010
title: Sign durable group items over their set time, and cache live reports under hourly Keystore RSA-OAEP envelopes
status: proposed
date: 2026-11-21
supersedes:
superseded_by:
---

# ADR-0010 — Sign durable group items over their set time, and cache live reports under hourly Keystore RSA-OAEP envelopes

> Proposed for Antoine's review with the threat model's §11 (WI-040). It adds to
> [ADR-0004](ADR-0004-crypto-design.md) and [ADR-0009](ADR-0009-group-frames-and-join.md) and
> replaces neither: everything they decide still holds except where this record says otherwise.

## Context

Release 1.1 ([extended spec](../SPEC-EXTENDED.md) F20–F27) needs two things the accepted records
rule out. The threat model's 1.1 section ([§11](../threat-model.md)) explains both.

- **A rally point outlives the epochs.** ADR-0009 signs every inner group message over
  `group id ‖ epoch ‖ body`, and a phone keeps 5 hourly epoch keys. A rally point can be set days
  before the event and must reach a member who was out of range all that time, and new joiners. Only
  the member who set it could re-send it, and only while their phone is nearby.
- **The report cache has to be written while the phone is locked.** ADR-0004 allows only Tink
  primitives, and §5.6 seals storage with a Keystore key created with `setUnlockedDeviceRequired`.
  That key cannot be used at all while the phone is locked, and a phone at a protest is locked most
  of the time (WI-077 hit this for stored settings). A cache sealed that way could not be written
  when it matters.
- **0.2.0 phones are in the field** with ADR-0009's inner layout. Any new group item reaches them.

## Decision

**Durable group items.**

- Inner types `0x10`–`0x1F` are **durable**. Their Ed25519 signature covers
  `"echo durable v1" ‖ group id 16 ‖ the 133 signed bytes` (sender key id, inner type and body, as
  in ADR-0009), **without the epoch**. Each durable body carries its own set time (u32 minutes),
  and the newest set time of an item wins.
- Any member may re-wrap a durable item, byte for byte as signed, in a current-epoch frame. The
  receiver verifies the original author's signature; the frame's epoch says only when it was
  re-sent.
- The first durable type is the rally point (WI-042). Roster entries may follow under the same
  rule, once F-008's "left" message is designed.
- Every other inner type, including 1.1's phrases and check-ins, stays bound to its epoch exactly as
  ADR-0009 lays out. The member-added frame `0x06` is unchanged.
- A 0.2.0 phone does not know `0x10`–`0x1F`: its decoder refuses the inner message, so it shows
  nothing, and it relays the frame like any group frame it cannot read.

**Report cache envelope.**

- One bucket per UTC hour. Each bucket has an Android Keystore **RSA-OAEP (SHA-256, MGF1-SHA-256)**
  key pair created with `setUnlockedDeviceRequired`, hardware-backed where the phone has it.
  Bucket keys are created ahead of time while the phone is unlocked.
- Each cache entry is sealed with a fresh XChaCha20-Poly1305 data key (Tink), and the data key is
  wrapped to the bucket's public key. Encrypting needs only the public key, so it works while the
  phone is locked; unwrapping needs the private key, so only after an unlock.
- A bucket's Keystore key is deleted once the last entry it can hold has expired (at most 3 hours
  after the bucket ends). Deleting the key, not the file, is what makes expired entries unreadable.
  The panic wipe deletes every bucket key first.
- The platform's RSA-OAEP is allowed in `:core:crypto`, for wrapping cache data keys only, in
  addition to Tink. Positions are never cached.

## Consequences

**Good**

- A member who was away all day still gets the rally point from anyone in the group, and a
  newcomer gets it at join, with the setter's signature intact.
- The map survives a restart, even one that happens while the phone is locked, and an expired
  hour becomes unreadable for good, even from a storage image taken earlier.
- No new wire version: the outer group frame is unchanged, and 0.2.0 phones ignore durable items
  rather than mis-reading them.

**Costs and risks**

- **A durable item can be replayed** by any member, or by anyone who later holds the group key,
  until the group expires. Newest-set-time-wins means a replay cannot roll an item back, only repeat
  it.
- **A durable item is not forward-secret in the way epoch items are**: its signature does not
  expire with the epoch. Its encryption still does, because each re-wrap uses the current epoch key.
- **A second asymmetric primitive** (RSA-OAEP) enters `:core:crypto`. It is single-block OAEP on
  local data with no oracle, not the PKCS#1 v1.5 mode Bridgefy was broken through, but it widens
  what the 1.1 review must check (threat model §8).
- **On a phone with a software-only Keystore**, a storage image taken before a wipe may keep the
  bucket keys. The app checks the key's security level, and settings say which case applies.
- **0.2.0 members see no rally point** until they update. Acceptable: it is a 1.1 feature.

## Alternatives considered

**Keep every epoch key for the group's life.** The rally point would stay readable. Lost because
it throws away the hourly forward secrecy for everything else the group sends (threat model §5.3,
A3).

**Have the setter re-send the rally point every hour.** No new signature rule. Lost because it
only works while the setter's phone is nearby and on, which is exactly when it is needed least.

**Seal the cache with the same `setUnlockedDeviceRequired` AES key as stored settings.** No new
primitive. Lost because the key cannot be used while the phone is locked, so the cache would be
empty after the restarts it exists for.

**A Keystore key without `setUnlockedDeviceRequired`.** Works while locked. Lost because a locked,
seized phone could then decrypt its own cache under forensic tools (A3).

**A Tink hybrid (HPKE) envelope.** Would keep the primitives to Tink. Lost because the private key
would then live outside the Keystore, so deleting it would not crypto-erase flash copies.

## Revisit triggers

1. The outside 1.1 review (threat model §8) finds a problem in either construction.
2. Field use shows members relying on group items older than the group's lifetime, which would mean
   circles (WI-069) are needed rather than more durable types.
3. Android offers a Keystore asymmetric key usable for encryption while locked and decryption only
   after unlock, in a form other than RSA.

## Admission check

Against [the rule](README.md): (1) constrains future work: the rally point, the cache, and any
later durable type ✅ · (2) real alternatives were rejected with stated reasons ✅ · (3) reversing it
after 1.1 ships means a wire change and a cache migration ✅ · (4) not owned by another document:
the threat model states the risks, this record the constructions ✅ · (5) not an implementation
detail: it fixes signed bytes and allowed primitives ✅.
