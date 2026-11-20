---
id: WI-075
title: Keep sharing your position while Echo is off screen
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-19
updated: 2026-11-19
related: [F-004, FR-10.1, FR-10.4, NFR-10, WI-020, WI-028]
epic: WI-029
children: []
---

## Proposal (rationale)

Position sharing pauses when Echo leaves the screen (finding F-004): the fix is read only while the app is visible, so a phone in a pocket goes stale for its group after 2 minutes, which is exactly when sharing matters. The design (patterns/PositionSharing) expects sharing to go on behind the notification. Android lets a foreground service of type `location`, started while the app is visible, keep reading the fix with the screen off, without `ACCESS_BACKGROUND_LOCATION`.

Needed before the field test (WI-028).

## Decision

`accepted` — 2026-11-19, Antoine ("position sharing = yes"): a `location` foreground service while sharing is acceptable under NFR-10; `ACCESS_BACKGROUND_LOCATION` stays forbidden.

## Plan

### Requirements

- While at least one share is running and location is granted, the mesh service runs as `connectedDevice|location` and hands the fix to the mesh, screen on or off, with the 30-second restamp `ShareOwnPosition` uses.
- The location type is added only from a user's tap with the app visible (starting to share), never by the service on its own; it is dropped as soon as no share is running, by stop, time up, group gone, mesh off or wipe.
- `ACCESS_BACKGROUND_LOCATION` can never be merged into the app: `checkManifestPermissions` forbids it.
- The fix stays in memory (FR-12.2); nothing new is logged or stored.
- The notification stops saying "Updates while Echo is open".
- Without the service (simulated radio, or Android refusing it), sharing falls back to on-screen updates as today.

### Impacts

- NFR-10 in `docs/SPEC.md` gains the decision. Manifest: `FOREGROUND_SERVICE_LOCATION` and the service type. `app/build.gradle.kts`: the forbidden set. `MeshService`, `App`, `ui/groups/Sharing.kt`, `ui/groups/Groups.kt`. Strings in three languages. Design: patterns/PositionSharing, components/SystemNotification.
- Threat model: no change in what is kept; the position already leaves the phone, encrypted to the group, while sharing.

### Approach

- `App.startSharing` starts sharing in the mesh and then starts the service again, so `onStartCommand` re-promotes it with the location type while the app is visible. The service watches the sharing list and re-promotes itself as `connectedDevice` only when it empties.
- `MeshService.locating` tells `ShareOwnPosition` the service has the fix, so the two never both send.

### Acceptance criteria / tests

- CI builds the app; `checkManifestPermissions` passes and fails if `ACCESS_BACKGROUND_LOCATION` is added (forbidden list).
- On a phone (queued with the other phone checks): start sharing, lock the phone for 5 minutes, and a second phone in the group keeps a fresh marker; stop sharing from the notification and the location indicator goes within seconds.

### Out of scope

- Sharing that starts while Echo is off screen: never, by design.
- Battery tuning of the location interval: the field test (WI-028) measures it with field diagnostics.

## Execution

2026-11-19.

- `App.startSharing` starts the share in the mesh, then starts `MeshService` again. Its `onStartCommand` promotes it to `connectedDevice|location` when a share runs and location is granted, and falls back to `connectedDevice` alone if Android refuses. A collector on the sharing list re-promotes it to `connectedDevice` when the last share ends; the mesh going off stops the service, and the locator with it.
- The locator reads `OwnPosition.updates` and restamps every `MeshService.RESTAMP` (30 s), as `ShareOwnPosition` did. `MeshService.locating` makes the on-screen `ShareOwnPosition` stand down while the service has the fix; it still runs with the simulated radio, where there is no service.
- Manifest: `FOREGROUND_SERVICE_LOCATION`, the service type `connectedDevice|location`. `app/build.gradle.kts` adds `ACCESS_BACKGROUND_LOCATION` to the forbidden set, so NFR-10 is now a gate rather than a sentence.
- The notification says "Updating for your group · stops in …" in all three languages. NFR-10 in the spec records the decision; patterns/PositionSharing and components/SystemNotification say the same.

- 2026-11-20, phone check (AC-15): the marker showed and Stop in the notification ended the share in 3 s, but the marker vanished as soon as the sharing phone locked. The service's location request was in place the whole time and received no fix in 12.5 minutes: the phone stood still, and the 5 m distance filter held every update back. `OwnPosition.updates` now starts with the last known fix when it is under 2 minutes old and asks for one current fix, and the service asks for updates with no distance filter. Retested: locked for 9 minutes, the other phone's marker stayed fresh, updated about every 30 s (14 fixes in the first 6.5 minutes, none before the fix).

## Review
