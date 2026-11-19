---
id: ADR-0009
title: Lay out group frames as signed, padded inner messages, and join over a fixed-size, never-relayed join frame added to wire version 1
status: proposed
date: 2026-11-19
supersedes:
superseded_by:
---

# ADR-0009 — Lay out group frames as signed, padded inner messages, and join over a fixed-size, never-relayed join frame added to wire version 1

> Proposed on 2026-11-19 for Antoine's acceptance. Phase 2 code (WI-017, WI-018) is built on it in
> the meantime, so a change here changes that code.

## Context

[ADR-0004](ADR-0004-crypto-design.md) chose the constructions: group frames are sign-then-encrypt
under an hourly ratcheted epoch key, with no group id on the air, and a newcomer joins through a
`Noise_NKpsk0` handshake over one direct radio hop. [ADR-0005](ADR-0005-wire-format.md) fixed the
outer group frame at 240 bytes with 197 bytes of plaintext and left the inner layout to the group
work. Threat model §5.3 and §5.5 add the rules: the inner message names its sender by member key and
is signed over the group context and epoch, a joiner gets only the current epoch key, and every join
is visible to the group.

Forces:

- **One encoding per message**, as everywhere on the wire: fixed offsets, no length read off the air,
  padding that must be zero, so the decoder stays a fixed-offset reader and the fuzz oracle holds.
- **Nothing learnable from the outside**: every group frame and every join message is one size.
- **The roster does not fit one frame.** A joiner needs every member's key and nickname, 65 bytes
  each, and a frame is 240 bytes.
- **No phone runs version 1 yet** (0.1.0 is not released), the same reason ADR-0008 used.

## Decision

**Epochs.** An epoch is a UTC hour (hours since the Unix epoch). `K(n+1) = HKDF-SHA256(K(n),
info "echo group ratchet")`, 32 bytes. A phone keeps the current epoch and the 4 before it (the
longest report life is 3 hours) and deletes older keys. The frame key is `HKDF-SHA256(K(n), info
"echo group frame")`.

**Group frame** (kind `0x04`, unchanged outside): `version ‖ 0x04 ‖ hop budget ‖ nonce 24 ‖
XChaCha20-Poly1305(frame key, nonce, inner, aad = version ‖ kind)`. A receiver trial-decrypts with
each epoch key it holds. The 197-byte inner message is:

| Field | Bytes |
| --- | --- |
| sender key id: the first 8 bytes of SHA-256 of the sender's member public key | 8 |
| inner type | 1 |
| body, zero-padded | 124 |
| Ed25519 signature by the member key over `group id 16 ‖ epoch u32 ‖ the 133 bytes above` | 64 |

Inner types, version 1: `0x01` report (`report type 1 ‖ latitude i32 ‖ longitude i32 ‖ created u32
‖ lifetime u16`), `0x03` withdrawal (`target id 32 ‖ created u32 ‖ lifetime u16`), `0x06` member
added (`member key 32 ‖ nickname length 1 ‖ nickname 32 ‖ created u32 ‖ lifetime u16`). Votes and
positions (WI-019, WI-020) take the codes the threat model lists. Unused bytes must be zero; a
nickname longer than 32 UTF-8 bytes is refused before it is sent.

Non-members cannot open a group frame, so the relay holds and forwards it by message id for 185
minutes after first seeing it, under the same rate limits as everything else.

**Join frame**, new kind `0x06`, 240 bytes, never relayed (any hop budget but 0 is refused, like
the summary): `version ‖ 0x06 ‖ 0 ‖ session 8 ‖ step 1 ‖ Noise message 227`. The session is 8 random
bytes the joiner picks for this one join, so the two phones can tell their messages apart on a busy
link; it lives for one join and is never reused.

- Step 1, joiner to member: Noise message 1 (`e`, then 179 bytes of encrypted payload: joiner's
  member key 32 ‖ nickname length 1 ‖ nickname 32, zero-padded).
- Step 2, member to joiner: Noise message 2 (`e`, then 179 bytes: group id 16 ‖ name length 1 ‖ name
  32 ‖ expiry minute u32 ‖ epoch u32 ‖ epoch key 32 ‖ roster size 1, zero-padded).
- Steps 3 and on: Noise transport messages of 211 bytes of plaintext each, holding up to 3 roster
  entries (`member key 32 ‖ nickname length 1 ‖ nickname 32`), until the roster size is reached.
  A roster holds at most 48 members, so at most 16 roster steps.

**The QR code** holds `echo-join:1:` and base64url of the member's one-time X25519 public key (32)
and a random 128-bit token (16). The Noise PSK is SHA-256 of `"echo join psk" ‖ token`; the
prologue is `"echo join v1"`. The member's phone answers the first valid step 1 for that code and
then destroys its one-time key, so a second scan of the same code fails (AC-25). It then sends a
member-added group frame, so every member sees the join (FR-8.2).

## Consequences

**Good**

- Every group and join message is one size, so a sniffer learns "a group frame" or "a join" and
  nothing more, and the decoder and fuzzer keep their fixed-offset form.
- The roster and the group key travel only inside a forward-secret Noise session.
- Members learn exactly who sent a frame; non-members learn nothing about the sender or the group.

**Costs and risks**

- **A join is visible on the air as a join**: a burst of `0x06` frames between two phones. The
  threat model already accepts that the app's presence is visible.
- **124 bytes of body** caps what a group message can carry; position sharing has to fit.
- **A member key id of 8 bytes** can collide between members of one group with negligible odds; a
  collision makes a frame unverifiable, not misattributed, because the signature is checked.
- **A report from a member seen before their member-added frame** cannot be attributed and is not
  shown until it arrives; it is still relayed.
- **Version 1 changes again**, accepted for the same reason as ADR-0008: nothing has shipped. From
  0.1.0 on, a new kind is a new version.

## Alternatives considered

**Variable-size join messages.** Simpler code. Lost because the length would tell a sniffer how big
the group is.

**The roster in a group frame burst after joining.** No transport messages. Lost because the roster
would then cross the mesh, where a joiner could miss frames, and the mesh would carry nicknames to
every relay even if encrypted.

**Group id on the inner message instead of in the signature.** Lost because it spends 16 bytes the
receiver already knows from the key that opened the frame.

## Revisit triggers

1. Groups need more than 48 members, or a group message needs more than 124 bytes.
2. Field tests (WI-028) show joins fail on a busy link, where the session id is not enough.
3. 0.1.0 ships: from then on a new kind or inner type is a new version.

## Admission check

Against [the rule](README.md): (1) constrains future work ✅ every group message and every join
follows it; (2) more than one serious option ✅ see Alternatives; (3) costly to reverse ✅ once
phones run it, a change is a new wire version; (4) not owned by another document ✅ ADR-0005 left the
inner layout open, and the threat model sets rules, not bytes; (5) affects more than one module ✅
`core/crypto`, `core/mesh`, `app`.
