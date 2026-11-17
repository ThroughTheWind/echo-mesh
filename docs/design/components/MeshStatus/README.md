# Mesh status

**Purpose:** answer "can my phone hear anyone, and when did the last report arrive?" at a glance. It is also the mesh's single always-visible on/off control (SPEC FR-3.1, FR-3.2, FR-3.3a, FR-3.5).

**Status:** Spec · **Compose:** `MeshStatusChip(state, onClick)`, `MeshSheet(state, onToggle, onFix)`.

## Chip states

| State | Looks like | Tap |
|---|---|---|
| On | `radar` in `primary`, **12** nearby · 8s | Opens the mesh sheet |
| On, crowd | **20+** nearby · 1s (counts cap at 20+: precision past that is false) | Sheet |
| Searching | hollow dot, "Looking for phones" (first 30 s after turning on) | Sheet |
| Alone | `blocked-container`, warning icon, "No phones nearby · 4 min" (nobody heard for 1 min; the age is since the last report received) | Sheet |
| Off | `surface-container-highest`, "Mesh off · **Turn on**" (first-launch default) | Turns the mesh on ([Mesh on and off](../../patterns/MeshOnOff/README.md)) |
| Needs a fix | `error-container`, "Bluetooth off · **Fix**" or "Permission needed · **Fix**" | Opens the Android setting or the permission prompt |
| Relaying stopped | `error-container`, "Relaying stopped · **Fix**": the service died while the mesh was meant to be on, usually an OEM battery manager (FR-3.3a) | Opens the background-running help |

The count is the number of distinct rotating IDs heard in the last 60 s, which over-counts slightly when IDs rotate; the copy says "nearby", not "connected", and the sheet says "approximate".

## Sheet

- A switch row "Mesh is on · Relaying in the background · stops when you turn it off" (FR-3.3).
- Facts: phones in range (approximate), last report received, whether relaying is active.
- **Radios in use**, by capability name ("Short range, low power · Works with the screen off"). The MVP has one transport; a second would add a row. Never "BLE" in the UI: the only radio name shown is the Android setting the user must switch on ("Turn on Bluetooth").
- Honesty callout (threat model §7): "Anyone nearby with a radio receiver can tell your phone is running Echo. Nothing it sends carries your name, number or a fixed ID."

## Accessibility

64dp hit area even though the chip is 48dp tall. Content description reads the whole state ("12 phones nearby, last report 8 seconds ago").

## Rules

- Never shows peer identities or a list of peers (FR-3.2).
- Never hidden, including in discreet mode.
