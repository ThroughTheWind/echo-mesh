---
id: WI-018
title: Send encrypted group reports that everyone relays and only members read
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [F9, FR-5.4, FR-9.1, AC-13, AC-14, AC-16]
epic: WI-016
children: []
---

## Proposal (rationale)

Groups benefit from the whole crowd only if non-members relay their traffic without being able to read it or tell which group it belongs to.

Phase 2 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Group reports end-to-end encrypted to the group and authenticated as from a member (FR-9.1).
- Fixed-size frames, no group id on the air, checkable by non-members before relaying (FR-5.4, threat model §5.3).
- Ratcheted group key as ADR-0004 says; after leave or expiry old reports cannot be read (AC-16).

### Impacts

- `core/crypto`, `core/mesh`, the wire format (a new frame kind, versioned per WI-003).

### Approach

- Reuse the relay of WI-009 unchanged: group frames are just another validated frame kind.

### Acceptance criteria / tests

- AC-13: a group report from A shows on B in the verified style.
- AC-14 (L): a phone outside the group relays it but cannot read it.
- AC-16: after leaving or expiry the group's keys are gone and its reports cannot be read.
- The fuzz target covers the group frame.

### Out of scope

- Votes: WI-019. Position sharing: WI-020.

## Execution

2026-11-19, the core half, built development-first while the phones are away.

- Group frames follow [ADR-0009](../../decisions/ADR-0009-group-frames-and-join.md) (accepted 2026-11-19): a 197-byte inner message signed by the sender's member key over the group id and epoch, sealed with XChaCha20-Poly1305 under the hour's key, no group id on the air, every frame the same size. Reports, withdrawals and member-added messages are the three inner types.
- The relay checks a group frame's layout, holds it by message id for 185 minutes and relays it like any other frame (FR-5.4); `GroupChannel` opens the ones for this phone's groups, attributes each to a roster member, honours withdrawals only from the sender, and attributes frames that arrived before their sender's member-added once it comes. `MeshNode` shows group reports with their group and sender, and sends to a group through the same grace period and per-type rate limit as public reports.
- Tests: `GroupBookTest` (AC-14 and AC-16 at the codec level, signatures bound to group and epoch, five groups at most, keys ratchet hourly); `GroupMeshTest` on loopback: a group report crosses a phone outside the group, which relays it and shows nothing (AC-14), arrives attributed to its sender (AC-13 in code), and is withdrawn everywhere; a newcomer added while another member is out of range is learned through the relay. `GroupFuzzTest` covers the inner decoder.

In the app (2026-11-19):
- The picker chooses the audience: everyone nearby, unverified, or one group, signed and encrypted. It defaults to the last choice, kept by group id in the sealed settings and never by name (FR-4.3), and the send button names the audience.
- Group reports are drawn in the verified style everywhere, on the map, in the list and in the detail (FR-9.2). The pin is solid in the group's colour with its monogram, TalkBack reads "from Sage in Crew", and the detail shows a lock with "From Sage, in Crew. Signed by Sage's phone and encrypted to your group."

Still to do on this item: the My groups filter (FR-9.3, with the WI-022 screens), and AC-13 and AC-14 on phones.

branch-merged-ok: the core half merges with PR #6 while the verified style and the phone checks stay open on this item.

## Review
