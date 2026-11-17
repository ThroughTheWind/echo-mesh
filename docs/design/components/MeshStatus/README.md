# MeshStatus

Can my phone hear anyone, and when did the last report arrive? Always visible, top left of the map; also the mesh's on/off control (SPEC F3, FR-3.1, FR-3.2, FR-3.5).

**Compose:** `MeshStatusChip(state, onClick)`, `MeshSheet(state, onToggle, onFix)`.

## Chip states

| State | Looks like | Tap |
|---|---|---|
| On | `radar` in `primary`, **12** nearby · 8s | Opens the mesh sheet |
| On, crowd | **20+** nearby · 1s (counts cap at 20+: precision past that is false) | Sheet |
| Searching | hollow dot, "Looking for phones" (first 30 s after turning on) | Sheet |
| Alone | `blocked-container`, warning icon, "No phones nearby · 4 min" (nobody heard for 1 min; the age is since the last report received) | Sheet |
| Off | `surface-container-highest`, "Mesh off · **Turn on**" (first-launch default) | Turns the mesh on directly |
| Needs a fix | `error-container`, "Radio off · **Fix**" or "Permission needed · **Fix**" | Opens the Android setting or permission prompt the adapter names |

The count is the number of distinct rotating IDs heard in the last 60 s, which over-counts slightly when IDs rotate; the copy says "nearby", not "phones connected", and the sheet says "approximate".

## Sheet

- Big switch row "Mesh is on · Relaying in the background · stops when you turn it off" (FR-3.3).
- Facts: phones in range (approximate), last report received, reports relayed in the last 10 min.
- **Radios in use**, by capability name from each transport adapter ("Short range, low power · Works with the screen off"; "Longer range, faster · Turn on Wi-Fi to use it"). Never "BLE" or "Wi-Fi Direct" in the UI; the only radio name shown is the Android setting the user must switch on.
- Honesty callout: "Phones around you can tell this app is running. They can't tell it's you: the ID your phone sends changes every few minutes." (Wording from the threat model.)

## Rules

- Never shows peer identities or a list of peers (FR-3.2).
- The chip is the mesh's single always-visible control (FR-3.1): it is never hidden, including in discreet mode.
- 64dp hit area even though the chip is 48dp tall.
