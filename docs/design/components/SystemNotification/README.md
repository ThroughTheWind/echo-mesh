# System notification

**Purpose:** the persistent notification Android requires while Echo relays in the background (SPEC FR-3.3), and the out-of-app indicator that you're sharing your position (FR-10.4).

**Status:** Spec · **Compose:** not Compose; `NotificationCompat` built in `:app`, styled by these rules.

## Variants

| State | Icon | Title | Text | Actions |
|---|---|---|---|---|
| Mesh on | `radar` | Mesh on | "12 phones nearby · relaying" | Turn off |
| Sharing position | `share_location` | Sharing your position with Crew | "Updating for your group · stops in 1 h 42" | Stop sharing, Turn mesh off |
| Discreet mode | a neutral `sync` glyph | Running | "Tap to open" | none |
| Lock screen (always) | app icon | Content hidden | none | none |

## Rules

- One notification only, on the `connectedDevice` foreground service; low importance, no sound, no vibration, no heads-up.
- Lock-screen visibility is **secret**: nothing about the mesh, groups or sharing shows on a locked phone.
- Discreet mode replaces title, text and icon with neutral ones (SPEC FR-13.1). Discreet mode does not disguise the launcher name or icon (FR-13.2), so the app name still shows in the notification header.
- No counts that change every second: update at most every 30 s, so the shade doesn't flicker in a pocket.
- "Turn off" stops all radio activity and position sharing, and removes the notification.

## Accessibility

Actions are real notification actions with verb labels; the title alone says the state.
