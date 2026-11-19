---
id: WI-074
title: Build the settings screen as designed
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-19
updated: 2026-11-19
related: [F-005, FR-1.3, FR-13.1, FR-18.1, FR-17.1, FR-11.1, WI-025, WI-026, WI-036, WI-037]
epic: WI-029
children: []
---

## Proposal (rationale)

There is no settings screen (finding F-005). The design specifies one ([screens/SettingsScreen](../../design/screens/SettingsScreen/README.md)), and items keep placing their controls elsewhere or not at all: What Echo protects sits on the map's top bar instead of under Settings (FR-1.3), the in-app language override has nowhere to live (FR-18.1), field diagnostics hide in the mesh sheet, and the theme, large pins and haptics preferences the design system already supports (`DisplayPrefs`) are not reachable, so `MainActivity` passes only discreet mode. Without the screen, 1.0 ships preferences the user cannot change.

Phase 4 of the [roadmap](../../ROADMAP.md), promoted from F-005.

## Decision

`accepted` — 2026-11-19, Antoine's standing instruction to continue through each phase; the screen was already specified in the design he accepted.

## Plan

### Requirements

- One scrolling screen, sections as designed: Mesh, Groups and maps, Display, Privacy, Feedback, Panic wipe, About. Every setting applies at once; no Save.
- Mesh: on/off; "Keep running in the background" (opens Android's battery optimisation setting); Field diagnostics (opens its screen).
- Groups and maps: Groups (the groups screen); Maps (the get-a-map sheet, until a Maps screen exists).
- Display: theme (Auto, Dark, Sunlight) in a radio sheet; discreet mode; large pins. A setting discreet mode overrides shows "Off while discreet mode is on".
- Privacy: What Echo protects (FR-1.3).
- Feedback: haptics (Off, Light, Strong) in a radio sheet.
- Language: on Android 13+, opens Android's per-app language setting (FR-18.1); below, says it follows the phone's language.
- Panic wipe: one row in `error`, opening the same hold sheet as the map.
- About: version, licence, source, security contact, as text. Nothing opens the network.
- Theme, large pins and haptics persist in sealed settings and reach `StreetTheme`.
- The map's top bar trades its What Echo protects button for a Settings button.
- Every new string in English, French and Spanish (`locales` gate).

### Impacts

- `core/data/Settings` gains three fields (defaults keep old files readable).
- `app`: `MainActivity` builds `DisplayPrefs` from settings; `ui/settings/Settings.kt`; `EchoApp` gains the screen; the map's top bar changes.
- Design: `screens/SettingsScreen` (which rows exist now), `screens/MapView` (top bar).

### Approach

- Theme and haptics are stored as their enum names, read back with a fallback to the default, so `core/data` stays free of the design system.
- No navigation library: one more `Screen` in the existing `when`. Settings opens What Echo protects, Groups and Diagnostics, and Back from those returns to Settings when they were opened from it.

### Acceptance criteria / tests

- `SettingsStoreTest`: the new fields round-trip, and a settings file written before them still loads.
- CI builds the app and lint passes; `locales` passes.
- On a phone (queued with the other phone checks): each row does what it says; theme, large pins and haptics survive a restart; discreet mode shows the overridden rows as off.

### Out of scope

- The Maps screen (list, size, delete, choose; FR-2.6): the WI-012 follow-up. The Maps row opens the get-a-map sheet until then.
- Default position-sharing duration and nearby hazard buzz: neither feature exists yet, so neither gets a row.
- The open-source licences list: needs a generated licence report; follow-up when 1.0 packaging is done (WI-038's release work).
- "Also delete maps" stays in the wipe sheet, where WI-024 put it.

## Execution

2026-11-19.

- `core/data/Settings` gains `theme`, `largePins` and `haptics`, stored by name; `Settings.displayPrefs()` in `MainActivity` turns them into the design system's `DisplayPrefs`, falling back to the default for a name it does not know. `SealedFilesTest` checks they round-trip and that a file written before them still opens.
- `ui/settings/Settings.kt` is the screen. The theme and vibration rows open radio sheets; large pins and vibration show "Off while discreet mode is on" and are disabled while it is; the theme row says it is discreet. The background row reads `PowerManager.isIgnoringBatteryOptimizations` again on every resume, so it shows the result after Android's screen.
- Turning the mesh on from Settings goes back to the map with a request (`meshOnRequested`), so the Nearby devices and Notifications sheets still come before Android's dialogs. Off happens in place.
- The map's top bar trades the shield for a gear; What Echo protects is under Settings > Privacy. Back from groups, What Echo protects and diagnostics returns to Settings when Settings opened them.
- `GetMapSheet` became `internal` so the Maps row can open it.
- The new strings are in English, French and Spanish, generated as in WI-037; `locales` passes.
- Design: `screens/SettingsScreen` gains "As built", `screens/MapView` the top bar as it is.

## Review
