---
id: WI-025
title: Discreet mode
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [F13, FR-13.1, FR-13.2, FR-13.3, FR-3.3]
epic: WI-023
children: []
---

## Proposal (rationale)

In a tense moment a bright screen, a vibration or an obvious notification can single a person out.

Phase 3 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- One switch: darkest theme, reduced brightness, no sound or vibration, neutral notification text and icon (FR-13.1).
- Launcher name and icon are not disguised (FR-13.2).
- A Quick Settings tile the user can add, which only toggles discreet mode and shows on or off (FR-13.3).

### Impacts

- `core/designsystem` (the Discreet theme exists), `app` (notification, haptics).

### Approach

- Drive everything from `DisplayPrefs.discreet`.

### Acceptance criteria / tests

- Every item of FR-13.1 verified on a reference phone, including the foreground-service notification text.

### Out of scope

- Disguising the app: rejected by FR-13.2, nothing deferred.

## Execution

2026-11-19, built development-first while the phones are away.

- `Settings.discreet`, sealed with the rest, drives `DisplayPrefs.discreet` as planned: the Discreet theme, window brightness at `discreet-brightness`, haptics off (the hold-to-wipe tick included) and large pins off all come from the theme, which already had them.
- The eye button on the map's top bar turns it on and off, one tap, no confirm. Onboarding's "Before you go" says where it is and that it doesn't hide the app (FR-13.2).
- The mesh notification in discreet mode is "Echo", "Running · Tap to open", with a plain dot for its icon; it says nothing about the mesh, groups or sharing, and keeps Turn off. While sharing it drops Stop sharing too, which would say what is going on; the in-app bar keeps its Stop.
- The "Echo discreet" Quick Settings tile (`DiscreetTile`) toggles it and shows On or Off only. Echo never adds or suggests it. Settings are sealed under a key that needs the phone unlocked, so on a locked phone the tile shows as unavailable and a tap asks to unlock first, rather than guessing or overwriting the settings with defaults.
- The launcher name and icon are unchanged (FR-13.2).
- No unit test: everything here is Android UI and services. The checks are FR-13.1's items on a reference phone.

Still to do on this item: every item of FR-13.1 verified on a reference phone, including the notification text and the tile on a locked phone.

branch-merged-ok: the code merges with PR #6 while the phone check stays open on this item.

## Review
