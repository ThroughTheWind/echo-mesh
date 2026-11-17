# Discreet mode

**Purpose:** make the screen dim, still and silent, so it doesn't draw a glance over a shoulder (SPEC F13, US-16).

## Turning it on and off

- The eye button on the map's top bar, the Quick Settings tile "Echo discreet", or Settings > Display. The same control turns it off. No confirm, no animation.

## What changes

| | Normal | Discreet |
|---|---|---|
| Theme | Dark / Sunlight / Auto | [Discreet](../../foundations/Themes/README.md): black, grey, no accent hue, report colours desaturated |
| Brightness | System | Window brightness `discreet-brightness` (0.12) while Echo is in front |
| Haptics and sound | As set | None (FR-13.1); the app never plays sounds anyway |
| Motion | Functional | State changes only |
| Notification | "Mesh on · 12 phones nearby" | "Running · Tap to open", neutral icon |
| Large pins | As set | Off |

## What doesn't change

Pins keep their shapes, verified and unverified stay distinct, the mesh chip and the panic wipe stay where they are. The launcher name and icon are not disguised (FR-13.2).

## Rules

- Discreet is not disguise; the copy never says it hides the app.
- Entering discreet mode never drops a pending action (an open sheet stays open, restyled).
