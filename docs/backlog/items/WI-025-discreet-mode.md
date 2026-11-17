---
id: WI-025
title: Discreet mode
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
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

## Review
