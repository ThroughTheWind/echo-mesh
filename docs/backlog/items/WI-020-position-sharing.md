---
id: WI-020
title: Share position with a group, opt-in and short-lived
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [F10, FR-10.1, FR-10.2, FR-10.3, FR-10.4, AC-15]
epic: WI-016
children: []
---

## Proposal (rationale)

Knowing where your group is matters at a protest, and it is also the most dangerous data the app handles. It must be off by default every time, encrypted to the group, and never stored.

Phase 2 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Per-group opt-in that turns itself off with the mesh, on app kill, or after the chosen duration (FR-10.1).
- Encrypted to the group, at a modest interval, each position expiring after a few minutes (FR-10.2).
- Received positions in memory only; latest per member with its age (FR-10.3).
- A persistent indicator with one-tap stop (FR-10.4).

### Impacts

- `core/mesh`, `core/crypto`, `app`. Reuses group frames from WI-018.

### Approach

- Interval and lifetime as constants to tune in the field test (WI-028).

### Acceptance criteria / tests

- AC-15: B shares, A sees it with its age; B stops with one tap and A's view goes stale then disappears.
- A test proves no position reaches storage (with WI-010's store inspected).

### Out of scope

- Background location: never (NFR-10). Tuning the interval: WI-028.

## Execution

2026-11-19, built development-first while the phones are away.

- A position is a fifth inner type of the group frame, `0x04` as the threat model's payload list orders it (§5.4): `latitude i32 ‖ longitude i32 ‖ second u8 ‖ created u32 ‖ lifetime u16`, signed by the member key. The second of the minute lets a receiver show an age to the second; the frame is the same 240 bytes as everything else, so nobody outside the group can tell a position from a report.
- `GroupChannel` keeps the latest position per member, by group and member key id, in memory only, never one older than the last, never your own, and drops each when it expires (`POSITION_LIFETIME`, 5 minutes) or the group goes.
- `MeshNode.startSharing(group, until)` sends every `SHARE_EVERY` (60 s) while there is a fix under `FIX_FRESH` (2 minutes) old, and stops by itself when time is up, when the mesh is turned off, or when the group is gone, saying why once (`sharingStopped`). A killed app shares nothing: none of it outlives the process. It never starts while the mesh is off.
- In the app: "Share my position" on the roster opens the sheet (30 min, 1 h, 2 h by default, until the mesh is off, with the callout), asking for location in context if it is missing; a bar per group under the map's top bar with one-tap Stop; members' markers on the map in the group's colour with their age, faded after 2 minutes and gone at expiry; "Sharing · seen 40 s ago" on the roster; and a snackbar saying why sharing stopped by itself.
- The phone's own fix is fed to the mesh only while the app is on screen (NFR-10, no background location; WI-075 later kept it coming with the screen off, by Antoine's decision). The platform sends nothing while you stand still, so the last fix is handed over again every 30 s while the listener runs.
- Tests: `PositionSharingTest` on loopback, AC-15 (B shares, A sees it with its age, Stop makes it age then disappear), stopping by time, by mesh off, never starting while off or outside the group, a stale fix not re-sent, and no position in any phone's kept records. `GroupBookTest` round-trips a position and refuses a 60th second; `GroupFuzzTest` has an `inner-position` seed.
- Deviations: the background notification line while sharing and the distance on the roster ("80 m") are not built: the first needs the mesh service's notification, the second the roster's member sheet, both WI-022. Relays hold a position frame for 185 minutes like any group frame, because they cannot read it; how much that costs is for the field test (WI-028).

Still to do on this item: AC-15 on two phones.

branch-merged-ok: the code merges with PR #6 while the phone check stays open on this item.

## Review
