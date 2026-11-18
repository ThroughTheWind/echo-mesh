---
id: ADR-0005
title: Put fixed-layout, versioned, uncompressed binary frames on the air, decoded by hand and fuzzed in CI
status: accepted
date: 2026-11-17
supersedes:
superseded_by:
---

# ADR-0005 — Put fixed-layout, versioned, uncompressed binary frames on the air, decoded by hand and fuzzed in CI

> Accepted by Antoine on 2026-11-18.

## Context

Every byte a phone receives comes from a stranger, and some strangers are hostile
([threat model](../threat-model.md) A4). Bridgefy was broken through its message format as much as
through its cryptography: a decompression bomb, a padding oracle hidden in gzip fields, and plaintext
recovered from compressed lengths (threat model §5.4, §6). A frame must also fit a slow radio: the
spec asks for a signed report of about 110–150 bytes (NFR-2), moved over a BLE connection.

Forces:

- [ADR-0004](ADR-0004-crypto-design.md) signs public frames with a key carried in the frame, and
  encrypts group frames to one fixed size. The signature must cover the exact bytes received, so the
  format must have one encoding per message: no optional field, no varint, no field order to choose.
- A relay changes one thing, the hop budget (spec FR-5.2), so that byte cannot be signed or hashed.
- The message id is the hash of the signed content (FR-5.2), so it must not depend on the hop budget.
- A decoder that throws, allocates from a length read off the air, or reads past the end lets one
  peer knock others off the mesh (NFR-3).
- The decoder must be fuzzed in CI (spec AC-12, crypto rule), and the fuzzer must be open source,
  run offline, and not ship in the app.

## Decision

**Envelope.** Every frame is `version (1 byte) ‖ kind (1 byte) ‖ hop budget (1 byte) ‖ body`, all
integers big-endian. Version 1 is the only version; a phone drops a version it does not know. The
hop budget is 0–7 and is the only byte outside the signature and the message id.

**Kinds and layouts, version 1.** Every kind has one exact length; anything shorter or longer is
dropped.

| Kind | Code | Body after the header | Bytes |
| --- | --- | --- | --- |
| Public report | `0x01` | public key 32 ‖ type 1 ‖ latitude i32 ‖ longitude i32 ‖ created u32 ‖ lifetime u16 ‖ signature 64 | 114 |
| Public vote | `0x02` | public key 32 ‖ target id 32 ‖ vote 1 ‖ created u32 ‖ lifetime u16 ‖ signature 64 | 138 |
| Withdrawal | `0x03` | public key 32 ‖ target id 32 ‖ created u32 ‖ lifetime u16 ‖ signature 64 | 137 |
| Group frame | `0x04` | nonce 24 ‖ ciphertext with tag 213 | 240 |

- **Positions** are signed 32-bit integers in units of 10⁻⁴ degree (about 11 m), so a report is
  coarse by construction (FR-4.4) and cannot carry a finer fix than the protocol allows.
- **Times** are minutes since the Unix epoch, UTC (FR-7.3); **lifetimes** are minutes, 1–180.
- **Report types** are the spec's catalog (FR-4.1), codes 0–8 in its order; votes are 1 (confirm)
  and 2 (dismiss). A new type or kind is a new version.
- **Signed bytes** are the frame without the hop budget and without the signature; the **message
  id** is SHA-256 of the frame without the hop budget. Both are slices of the bytes received.
- **Group frames** carry 197 bytes of plaintext inside the AEAD. Their inner layout belongs to the
  group work, and fits in that room.
- **No compression**, anywhere in `core/`. `checkCompressionImports` refuses `java.util.zip` and
  compression libraries in source; `gradle/forbidden-in-core.txt` refuses them on every `:core:*`
  runtime classpath.

**Decoder.** `WireCodec` in `:core:mesh` is written by hand. It checks the size against the
transport's limit before reading a field, reads only fixed offsets, never allocates from a value it
read, and returns a typed rejection instead of throwing. It checks layout and value ranges only;
expiry, de-duplication, signatures and meaning are checked above it, in the order of threat model
§5.4, before anything is stored or relayed.

**Fuzzing.** The decoder has a Jazzer fuzz test (Apache-2.0, test classpath only) whose oracle is:
never throw, and every accepted frame re-encodes to exactly the bytes received. CI fuzzes it for
two minutes on every pull request, and runs the same harness around a planted bug to prove the step
can fail. Seeds and findings are committed under `core/mesh/src/test/resources`, so every test run
replays them.

## Consequences

**Good**

- A public report is 114 bytes, inside the spec's estimate, and the largest frame is 240 bytes, so
  every kind fits one BLE L2CAP packet and an extended advertisement.
- One encoding per frame makes "verify the exact bytes received" and "re-encode equals input" the
  same property, which a fuzzer can check on millions of inputs.
- No length field read off the air, so no allocation or offset is ever attacker-controlled.
- The compression ban is mechanical in source and in dependencies.

**Costs and risks**

- Adding a field, a report type or a kind needs a new version, and phones on different versions
  cannot read each other's frames. Until 1.0 that is accepted; after 1.0 a phone should accept the
  previous version for a transition period, which is its own decision.
- Coarse positions (about 11 m) cannot be made finer without a new version.
- Lifetimes are capped at 180 minutes on the air; a longer report needs a new version.
- 32-bit minutes run out in the year 10136.
- The decoder is ours to keep correct, field by field. The fuzzer and the round-trip property
  are what keep it honest, and the outside review reads it (threat model §8).
- Fuzzing adds about three minutes to every CI run.

## Alternatives considered

**kotlinx.serialization (CBOR or ProtoBuf) on the air.** Already in the catalog and less code to
write. Lost because both formats allow several encodings of one message (field order, integer
widths, optional fields), so a signature over a re-encoding could differ from the bytes received,
and the generated decoders read lengths off the air. It stays available for local storage.

**Protocol Buffers with the official runtime.** Mature, with good tooling. Lost for the same
reasons, plus unknown fields being kept and passed on, which turns a relay into a carrier for data
it cannot check.

**A length-prefixed TLV format.** Extensible without a version bump. Lost because extensibility is
exactly what lets a hostile frame carry what the next version or the next parser misreads, and
because every length read off the air is an allocation an attacker controls.

**Compressing frames to save radio time.** Lost outright: the saving on 114 bytes of mostly keys and
signatures is negligible, and compression is the root of three of Bridgefy's breaks.

**libFuzzer through a native harness, or a property-based test library alone.** A native harness
cannot fuzz Kotlin; property tests with random inputs find shallow bugs but have no coverage
feedback. Jazzer gives coverage-guided fuzzing on the JVM through JUnit, with no native code in the
app. The unit tests keep a seeded random sweep as well.

## Revisit triggers

1. A transport's message budget is below 240 bytes (shrink the group frame size).
2. Field tests show 11 m positions are too coarse or too fine.
3. 1.0 ships and a second wire version is needed: decide how long a phone accepts the previous one.
4. Jazzer stops being maintained or open source (switch to another coverage-guided JVM fuzzer).
5. The group frame's inner layout does not fit 197 bytes.

## Admission check

Against [the rule](README.md): (1) constrains future work ✅ every frame, signature and relay
depends on it · (2) a real alternative was rejected with a stated reason ✅ · (3) reversing later
costs meaningfully more ✅ phones in the field would stop understanding each other · (4) not owned by
another document ✅ the threat model sets the constraints, this record fixes the format · (5) not an
implementation detail ✅ the layout is a contract between phones, not between classes.
