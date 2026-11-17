---
id: ADR-0004
title: Secure the mesh with signed frames, a ratcheted group key and a Noise join, built on Tink
status: proposed
date: 2026-11-16
supersedes:
superseded_by:
---

# ADR-0004 — Secure the mesh with signed frames, a ratcheted group key and a Noise join, built on Tink

## Context

The app relays reports between strangers' phones over short-range radio, with no server, for people
who may be targeted ([threat model](../threat-model.md), adversaries A1–A6). The brief forbids
home-made cryptography and requires a well-reviewed library and a published protocol design. The
best-known protest mesh app, Bridgefy, was broken twice: once for its home-made design (Albrecht et
al., CT-RSA 2021) and again after adopting libsignal, because the protocol around the library still
leaked metadata, compressed before encrypting, and left broadcasts unauthenticated (Albrecht,
Eikenberg, Paterson, USENIX Security 2022). A good library is necessary and not sufficient.

Forces:

- **Traffic is mostly one-to-many and delay-tolerant.** Reports are broadcast to "everyone nearby" or
  to a group, hop through phones that are not in the group, and arrive out of order or not at all.
  There is no 1:1 chat in the MVP.
- **Group keys must be exchanged in person** (brief), never through the mesh.
- **Radio budget is tiny.** A frame with its authentication must fit the transport's single-message
  budget (spec NFR-2), likely a few hundred bytes.
- **Crypto and protocol logic are plain Kotlin/JVM** and tested on the JVM with simulated hostile
  peers (`:adapters:loopback`), so the library must behave the same on the JVM and on Android.
- **Every dependency is open source and GPL-3.0-compatible**, with no Play Services, and the build
  should stay reproducible for F-Droid.

## Decision

**Library.** `:core:crypto` uses **Google Tink** (Apache-2.0) for every primitive:
Ed25519, X25519, ChaCha20-Poly1305 and XChaCha20-Poly1305, HKDF-SHA256, SHA-256. One artifact,
`tink-android`, on Android and in the JVM tests alike: it is a plain jar that runs on the JVM
(`TinkPrimitivesTest`), while the JVM-only `tink` artifact pulls in an HTTP client and native
Conscrypt, which `gradle/forbidden-dependencies.txt` refuses. No other crypto library is on the
classpath. No module outside `:core:crypto` calls a primitive.

**Protocols.** Three constructions, each from a published design, with nothing invented in between:

1. **Public reports and votes:** each is signed with a fresh **Ed25519** key used for that one
   message (RFC 8032). The key is kept in memory until the report expires, only to sign a
   withdrawal.
2. **Group traffic:** **sign-then-encrypt** with **XChaCha20-Poly1305** (random 192-bit nonce) under
   an epoch key; the inner signature is Ed25519 by the sender's member key. Epoch keys come from the
   group secret by an hourly one-way **HKDF** ratchet (RFC 5869), keyed to UTC time. No group
   identifier on the air: receivers trial-decrypt. All group frames have one fixed size.
3. **Joining a group in person:** the **Noise Protocol Framework** (revision 34) handshake
   `Noise_NKpsk0_25519_ChaChaPoly_SHA256`, over one direct radio hop. The responder's static key is a
   one-time X25519 key shown in a QR code with a random 128-bit token used as the PSK. The group's
   current epoch key and roster travel only inside that handshake.

**Rules that apply to all three.**
- No compression anywhere in the wire format.
- Verify (signature or AEAD) before any other processing, and before relaying.
- Every wire change bumps a version byte and adds fuzz seeds ([ADR-0005](ADR-0005-wire-format.md)).
- The Noise handshake is implemented in `:core:crypto` on Tink primitives and must pass published
  Noise test vectors (cacophony) for `NKpsk0` in CI before it is used.
- An independent review of all three (threat model §8) happens before 1.0.

## Consequences

**Good**

- One audited-by-use, pure-Java library with a small, misuse-resistant API, identical on the JVM and
  Android, so the loopback mesh tests exercise the real crypto.
- Every construction has a public specification that an outside reviewer already knows.
- No identity on the air and no 1:1 sessions removes Bridgefy's TOCTOU, MITM and social-graph
  classes of attack, rather than patching them (threat model §6).
- Group forward secrecy over hours: a phone seized today cannot decrypt traffic captured more than a
  few epochs ago.
- Small frames: about 100–150 bytes of authentication overhead, inside a BLE extended advertisement.

**Costs and risks**

- **We implement the Noise state machine ourselves.** No maintained Noise library for the JVM was
  found (`noise-java` describes itself as a reference implementation and has no releases). The
  pattern is small, and test vectors exist, but it is the first item for the outside review.
- **No post-compromise security and no member removal.** A member who leaks the group secret leaks
  every future epoch until the group expires. The answer is to re-form the group in person (spec
  FR-8.5). This is accepted for groups that live 24 hours by default and 7 days at most.
- **Public reports are Sybil-able.** Fresh keys per report mean anyone can mint unlimited "voices".
  The design limits the damage (threat model §5.7, §5.8) and the UI says so; it does not prevent it.
- **The ratchet depends on clocks.** Phones with very wrong clocks fail to read group traffic. The
  app keeps neighbouring epochs and warns when its clock disagrees with peers.
- **Tink is maintained by Google.** It is Apache-2.0 open source and does not depend on Play
  Services, but the dependency gate must allow `com.google.crypto.tink:tink-android` explicitly while
  blocking `com.google.android.gms`, `com.google.firebase` and the JVM `tink` artifact, so it cannot
  be a blanket `com.google` rule.
- **tink-android brings Gson** (Apache-2.0) for its JSON keyset format, which the app does not use.
- **tink-android needs API 23 and Java 11**, both inside the stack's minSdk 31 (ADR-0001).
- Joining needs a working one-hop radio link between two phones; if that proves unreliable, the
  revisit trigger below applies.

## Alternatives considered

**libsodium via Lazysodium** (MPL-2.0 wrapper, ISC library). The most reviewed constant-time
primitives available. Lost because it needs JNA and native libsodium binaries for each ABI and for
the JVM test host, which makes reproducible F-Droid builds and plain-JVM testing harder, for no
primitive we lack in Tink.

**Bouncy Castle** (MIT). Pure Java and complete. Lost because its low-level API makes misuse easy
and it is far larger than we need. It is the fallback if Tink ever drops a primitive we use.

**libsignal / the Signal protocol for every pair.** Designed for 1:1 and server-mediated groups. Lost
because the app has no 1:1 chat and no server, and Bridgefy shows that wrapping it around a
broadcast mesh still leaks (P2). Pairwise sessions would also need peer identities on the air.

**Sender Keys** (each member encrypts with their own key, distributed over 1:1 secure channels).
Lost because distributing them needs pairwise channels through the mesh (forbidden for keys) or
in-person meetings between every pair of members.

**MLS (RFC 9420).** Gives forward secrecy and post-compromise security and member removal. Lost for
the MVP because it needs ordered commits through a delivery service, which a partitioned,
serverless mesh does not have, and because its size and complexity outweigh its benefit for groups
that live at most 7 days. First candidate if member removal becomes a requirement.

**Group key directly in the QR code.** Simplest, and works without radio. Lost because a photo of
the QR gives silent, permanent read access to the group. With a one-time Noise join, a photo is
worth one visible join at most.

**Briar's in-person QR protocol (BQP), adopted as is.** A published mutual-scan key agreement for
two-party contacts. Lost because our join is one member admitting one newcomer to a group, which
`NKpsk0` expresses directly with a single scan; Briar's ideas (scan in person, then authenticate over
a local link) are kept. Briar announced maintenance mode in July 2026.

**Link-layer encryption between neighbours** (for example `Noise_NN` per connection). Lost for the
MVP: it hides nothing from an adversary who simply runs the app, cannot cover connectionless
advertising, and costs a handshake per encounter on a slow radio. Security lives end to end at the
message layer, and every link is treated as hostile.

**Proof-of-work on public reports.** Lost because it drains protesters' batteries far more than it
costs an adversary with a laptop.

## Revisit triggers

1. 1:1 chat, member removal, or groups that live longer than 7 days enter the spec (consider MLS or
   a pairwise protocol, and redo threat model §6 rows 8 and 15).
2. A maintained, reviewed Noise library for the JVM appears (replace the in-house state machine).
3. The outside review finds a flaw in any of the three constructions.
4. Tink drops Android support, a primitive we use, or its open-source licence.
5. Field tests show the one-hop QR join fails often (switch to a two-way QR exchange).
6. Frames do not fit the chosen transport's message budget with these signatures.

## Admission check

Against [the rule](README.md): (1) constrains future work ✅ · (2) a real alternative was rejected
with a stated reason ✅ · (3) reversing later costs meaningfully more ✅ (the wire format and every
stored key depend on it) · (4) not owned by another document ✅ (the threat model owns what is
defended, this record owns how and with what) · (5) not an implementation detail ✅.
