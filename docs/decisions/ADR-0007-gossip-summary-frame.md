---
id: ADR-0007
title: Swap what each phone holds as a salted Bloom filter in one fixed-size summary frame, added to wire version 1
status: proposed
date: 2026-11-18
supersedes:
superseded_by:
---

# ADR-0007 — Swap what each phone holds as a salted Bloom filter in one fixed-size summary frame, added to wire version 1

## Context

Spec FR-5.1 asks for epidemic gossip with anti-entropy: on meeting, phones exchange a set summary
and pull only what they lack, newest and most severe first, within a per-connection byte budget.
Pushing every frame on every meeting would cost a phone up to its whole store (256 frames, about
29 KB) per neighbour per meeting, over BLE, in a crowd where neighbours change all the time.

[ADR-0005](ADR-0005-wire-format.md) fixed version 1 with four kinds, one exact length each, no
optional field and no length read off the air, and says a new kind is a new version. A summary is a
new kind.

Forces:

- **Fixed layout.** A summary has to be one frame of one exact size, like every other, so the
  decoder stays a fixed-offset reader and the fuzzer's oracle (re-encode equals input) still holds.
- **Nothing stable on the air** (threat model §5.1). A summary is sent over the air, unencrypted, at
  least every few seconds. If two summaries of the same set looked alike, a sniffer could link a
  phone across its address rotations by what it holds.
- **Fits the frame budget**: at most 240 bytes, the largest frame (ADR-0005).
- **Cheap to answer**: a phone answering a hostile summary must spend little, and answer rarely.
- **No phone has ever run version 1.** 0.1.0 is not released; there is no field to stay compatible
  with.

## Decision

- **A summary frame**, kind `0x05`, 240 bytes: `version ‖ 0x05 ‖ hop budget (always 0) ‖ salt (16)
  ‖ Bloom filter (221 bytes, 1,768 bits)`. It is not signed, not stored and never relayed; any hop
  budget but 0 is rejected by the decoder.
- **The filter** sets 4 bits per held message id, taken as four 16-bit values, modulo 1,768, from
  SHA-256 of `salt ‖ id`. At 256 ids that gives about 3.7% false positives. A false positive only
  means one frame is not pushed this round; another neighbour, or the next round, sends it.
- **A fresh random salt per summary**, so two summaries of the same set share neither bits nor salt.
- **Exchange:** a phone sends its summary to a neighbour when they meet and every 15 s (±50%) after.
  The neighbour pushes back the frames whose ids are not in the filter: withdrawals first, then
  newest, then most severe, within 16 KB, and answers each link at most once per half period.
- **Added to version 1**, not a version 2, because no phone runs version 1 yet. From 0.1.0 on,
  ADR-0005's rule holds again: a new kind is a new version.

## Consequences

**Good**

- A meeting costs one 240-byte frame each way plus only what is missing, instead of every held frame.
- The frame keeps ADR-0005's properties: one size, fixed offsets, no length field, fuzzed with the
  same oracle, and the same size as a group frame.
- A sniffer cannot compare two summaries, and a summary names no phone.
- The relay's answers are bounded per link in bytes and in frequency.

**Costs and risks**

- **Summaries reveal the size of the set**, roughly, from how many bits are set. Phones near each
  other hold much the same set, so this says more about the crowd than about the phone; a phone
  that just arrived has a small set for a few seconds.
- **A hostile peer can send an empty summary** to have everything pushed to it, once per half period
  per link: at most 16 KB every 7.5 s, against the per-link frame budget it spends to ask.
- **False positives** grow past 256 ids; the store is capped at 256 for that reason.
- **Version 1 changed after ADR-0005 was accepted.** Accepted only because nothing has shipped.
- The salt costs one SHA-256 per held id per summary (256 hashes every 15 s per neighbour).

## Alternatives considered

**Push everything on meeting, no summary.** No new kind. Lost because a full store is about 29 KB
per neighbour per meeting over BLE, most of it duplicates the neighbour already holds.

**A list of truncated ids** (for example 26 ids of 8 bytes). Exact, no false positives. Lost because
240 bytes hold only 26 ids against a store of 256, so it would take ten frames to say what one
filter says, and fixed-count padding would show how full the store is exactly.

**A Bloom filter without a salt.** One hash per id, cached. Lost because identical sets give
identical filters, which links a phone across address rotations (threat model §5.1).

**Summary plus request kinds** (a phone asks for specific ids). Two frames per round and a second
new kind. Lost because "push what is not in the filter" achieves the same pull with one frame.

**Version 2.** Literal to ADR-0005's rule. Lost because there is no version 1 in the field to stay
apart from, so it would only renumber every seed and test.

## Revisit triggers

1. Field tests (WI-028) show summaries or their answers crowd out reports on BLE.
2. The store needs to grow well past 256 frames (false positives climb).
3. Captures on the reference phones (AC-19) show summaries can be linked across address rotation.
4. Group frames (WI-018) need to be summarised differently from public ones.

## Admission check

Against [the rule](README.md): (1) constrains future work ✅ every relay round and the radio adapter
depend on it · (2) a real alternative was rejected with a stated reason ✅ · (3) reversing later
costs meaningfully more ✅ once 0.1.0 ships it is part of the wire · (4) not owned by another document
✅ ADR-0005 fixed the format; this adds a kind to it · (5) not an implementation detail ✅ it is a
contract between phones.
