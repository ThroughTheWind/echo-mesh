---
id: WI-022
title: Build the group screens: create, join, roster, verified style and filters
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [F8, F9, FR-4.3, FR-8.4, FR-8.7, FR-9.2, FR-9.3]
epic: WI-016
children: []
---

## Proposal (rationale)

Groups are only safe if their screens make the risks plain: who can read, what a QR gives away, and which pins are verified.

Phase 2 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Create, join (scanner), show QR with its warning (FR-8.3), leave; nickname per group (FR-8.4).
- Roster of members seen recently with last-seen age, up to 5 groups (FR-8.7).
- Audience choice on the send button (FR-4.3); verified style and group colour (FR-9.2); filter (FR-9.3).
- Built from the design system's group components.
- Carried from WI-020 (2026-11-19): the member sheet with distance and "Show on map" (screens/GroupRoster), and the sharing line in the mesh notification while the app is in the background (patterns/PositionSharing, step 2).

### Impacts

- `app`, `core/designsystem`.

### Approach

- Screens land with the feature they expose; this item closes when all exist.

### Acceptance criteria / tests

- AC-13 end to end through the UI on two reference phones.
- The QR screen blocks screenshots (`FLAG_SECURE`).

### Out of scope

- Final copy review: WI-026.

## Execution

2026-11-19, built development-first while the phones are away. The screens landed with the features they expose, as planned: create, join by scanning, show the code under `FLAG_SECURE` with its warning, leave and rename with WI-017; audience choice and the verified style with WI-018; votes with WI-019; sharing with WI-020; muting with WI-021. This item adds what was left:

- The Everything / My groups filter (FR-9.3): a filter button bottom left on the map and a segmented button on Reports near you, the same state for both, shown only to a phone in a group. My groups hides every public pin, your own included.
- The roster shows members seen recently with their age, nothing older (FR-8.7): `GroupChannel` keeps, in memory only, when each member was last heard from (any group frame they signed, or their join), and forgets it after 15 minutes. Members not seen for 15 minutes drop off the roster; yourself and muted members stay. The groups list counts only members seen recently, and says when you share.
- A member's sheet shows "Sharing · seen 40 s ago · about 120 m north-east" and **Show on map**, which centres the map on them (carried from WI-020).
- Every member sees "Owl joined Crew" as a snackbar on the map (FR-8.2, patterns/QrJoinFlow step 5); the code's own phone keeps its guidance on the code screen.
- While sharing, the mesh notification reads "Sharing your position with Crew" and "Updates while Echo is open · stops in 1 h 42", with **Stop sharing** (FR-10.4, components/SystemNotification). The "while Echo is open" is honest about NFR-10: no background location. Whether a `location` foreground service may keep sharing behind the screen is F-004 in the findings, for Antoine.
- Tests: `RosterSeenTest` on loopback (seen joining, seen through the member-added, dropped after 15 minutes of silence).
- Known gap: a newcomer's roster starts with only themselves, because the join tells them who is in the group but not who was seen; the others appear as soon as they send anything.

Still to do on this item: AC-13 end to end through the UI on two phones.

branch-merged-ok: the code merges with PR #6 while the phone check stays open on this item.

## Review
