---
id: WI-024
title: Panic wipe in under two seconds
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [F11, FR-11.1, FR-11.2, FR-11.3, FR-11.4, FR-2.6, AC-17]
epic: WI-023
children: []
---

## Proposal (rationale)

If a phone is about to be seized, the person needs everything gone fast, without a gesture that fires by accident and without telling the mesh they wiped.

Phase 3 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Reachable in at most two actions from any screen; deliberate gesture (hold or slide) from the design system (FR-11.1).
- Destroy the storage key first, then delete data, groups, settings and optionally maps; stop the mesh (FR-11.2).
- Afterwards the app looks like a fresh install (FR-11.3); nothing is sent over the air (FR-11.4).

### Impacts

- `core/data` (key destruction from WI-010), `core/mesh`, `app`.

### Approach

- Time the wipe in an instrumented test on the slowest reference phone.

### Acceptance criteria / tests

- AC-17: the wipe completes in under 2 s, leaves a fresh install, and a radio capture shows nothing sent.

### Out of scope

- A duress PIN that wipes on entry: after 1.0.

## Execution

2026-11-19, built development-first while the phones are away.

- `App.wipe` destroys the Keystore sealing key first (`SealedFiles.destroy`, WI-010), so settings and every group record are unreadable at once, then deletes them; stops the mesh service; and runs `MeshNode.wipe`, which drops the pending report unsent, stops sharing with no message, turns the radio off without sending anything (FR-11.4) and destroys every group key in memory. It then deletes everything else the app wrote (files, cache, no-backup files), keeping the maps unless the user ticked "Also delete maps". `App.restartFresh` starts a new process on the first-launch screen with no "wiped" message (FR-11.3), so nothing in memory survives.
- The sheet follows patterns/PanicWipeConfirm: tap the wipe button, then hold the 88dp `HoldToConfirmButton` (new in `core/designsystem`) for 2 seconds, with the fill, the clipped second label, "Keep holding · 1.2 s", a tick every 0.5 s except in discreet mode, and a TalkBack custom action. The button is on the map's top bar and on every screen's back bar, so it is two actions from anywhere (FR-11.1). Onboarding's "Before you go" now says where it is.
- Tests: `PanicWipeTest` on loopback (the radio stops, the pending report and sharing are dropped, groups are forgotten, the other phone receives nothing afterwards and keeps its copies). `SealedFilesTest` already covers the key-first order.
- Deviations: the include-maps choice is a checkbox in the sheet, not a Settings > Privacy switch, because there is no settings screen yet; the design is aligned. The Quick Settings tile is not built: the in-app button already meets FR-11.1, and a tile needs a manifest service the permission gate has not reviewed. It is noted here for the 1.0 accessibility and street review.

Still to do on this item: AC-17 on the slowest reference phone (under 2 s, a fresh install afterwards, nothing on the air in a capture).

branch-merged-ok: the code merges with PR #6 while the phone check stays open on this item.

## Review
