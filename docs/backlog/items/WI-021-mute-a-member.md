---
id: WI-021
title: Mute a group member on this phone
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [FR-9.4, AC-24]
epic: WI-016
children: []
---

## Proposal (rationale)

An infiltrator or a member posting nonsense cannot be removed in the MVP. Muting hides them locally, so one person can protect their own view of the map.

Phase 2 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- A local action on a member hides their reports, votes and position on this phone only (FR-9.4).
- Stored encrypted with the group; cleared when the group is left or expires.

### Impacts

- `core/data`, `app` (roster).

### Approach

- Filter at display time; relaying is unchanged so the rest of the mesh is not affected.

### Acceptance criteria / tests

- AC-24: muting hides that member's reports, votes and position within one refresh.

### Out of scope

- Removing a member for everyone (re-keying): after 1.0.

## Execution

2026-11-19, built development-first while the phones are away.

- A muted member is kept by member key id with the group, in its sealed record (`GroupRecord` version 2, which adds the muted ids; a version 1 record reads as muting nobody), so it is cleared when the group is left or ends, with the keys. This is `core/mesh`'s record sealed by `core/data`'s `SealedFiles`, not a new store in `core/data` as the plan's impacts said: the record already holds the roster the mute refers to.
- `GroupChannel.mute` filters at display time, as planned: the member's group reports, group votes (on group and public reports) and position are hidden from what the screens read, at once; a muted member's confirmation no longer extends a report's time left on this phone. Relaying is unchanged. You cannot mute yourself. A muted member's public votes cannot be told from anyone's: they are unlinkable by design (threat model §5.7).
- `GroupInfo.roster` gives the screens each member's id, nickname and whether they are muted, never their key.
- In the app: tapping a member on the roster opens their sheet, "Muting hides Sage's reports, votes and position on this phone only. Sage stays in Crew and isn't told." with **Mute Sage**; a muted member reads "Muted · hidden on this phone" with **Unmute**, and the roster shows the "You can't remove someone" warning once anyone is muted.
- Tests: `MuteTest` on loopback (AC-24: a member's report, dismissal and position disappear on the muting phone within one refresh, a new report stays hidden, another member still sees everything, unmuting brings it all back), and `GroupRecordTest` for the kept mute and the version 1 record.

Still to do on this item: AC-24 on phones.

branch-merged-ok: the code merges with PR #6 while the phone check stays open on this item.

## Review
