# Mesh status

**Purpose:** answer "can my phone hear anyone, and when did the last report arrive?" at a glance. It is also the mesh's single always-visible on/off control (SPEC FR-3.1, FR-3.2, FR-3.3a, FR-3.5).

**Status:** Built · **Compose:** `MeshStatusChip(state: MeshChip, onClick)` in `core/designsystem/…/component/MeshStatus.kt`; the sheet is the app's `MeshSheet` (it names transports by capability, which the design system does not know).

## Chip states

| State | Looks like | Tap |
|---|---|---|
| On | `radar` in `primary`, **12** nearby · 8s | Opens the mesh sheet |
| On, crowd | **20+** nearby · 1s (counts cap at 20+: precision past that is false) | Sheet |
| On, few phones | `blocked-container`, warning icon, "Few phones · **2** nearby · 8s": fewer than 3 distinct phones heard in the last 5 minutes (extended spec FR-16.1; both values tuned in the field test). TalkBack adds "the map may be missing reports" | Sheet |
| Searching | hollow dot, "Looking for phones" (first 30 s after turning on) | Sheet |
| Alone | `blocked-container`, warning icon, "No phones nearby · 4 min" (nobody heard for 1 min; the age is since the last report received) | Sheet |
| Off | `surface-container-highest`, "Mesh off · **Turn on**" (first-launch default) | Turns the mesh on ([Mesh on and off](../../patterns/MeshOnOff/README.md)) |
| Needs a fix | `error-container`, "Bluetooth off · **Fix**" or "Permission needed · **Fix**" | Opens the Android setting or the permission prompt |
| Relaying stopped | `error-container`, "Relaying stopped · **Fix**": the service died while the mesh was meant to be on, usually an OEM battery manager (FR-3.3a) | Opens the background-running help |

The count is the number of distinct rotating IDs heard in the last 60 s, which over-counts slightly when IDs rotate; the copy says "nearby", not "connected", and the sheet says "approximate".

## Sheet

- A switch row "Mesh is on · Relaying in the background · stops when you turn it off" (FR-3.3).
- In the few-phones state, an amber callout first: "Few phones heard lately. The map may be missing reports: an empty map doesn't mean all clear." (FR-16.1).
- Facts: phones in range (approximate), last report received, whether relaying is active.
- **Radios in use**, by capability name ("Short range, low power · Works with the screen off"). The MVP has one transport; a second would add a row. Never "BLE" in the UI: the only radio name shown is the Android setting the user must switch on ("Turn on Bluetooth").
- A row "Field diagnostics · On / Off" opening [Field diagnostics](../../screens/FieldDiagnostics/README.md) (extended spec F17). Off by default; it sits here because it counts the mesh. When a settings page exists, it moves there.
- Honesty callout (threat model §7): "Anyone nearby with a radio receiver can tell your phone is running Echo. Nothing it sends carries your name, number or a fixed ID."

## Accessibility

64dp hit area even though the chip is 48dp tall. Content description reads the whole state ("12 phones nearby, last report 8 seconds ago").

## Rules

- Never shows peer identities or a list of peers (FR-3.2).
- Never hidden, including in discreet mode.
