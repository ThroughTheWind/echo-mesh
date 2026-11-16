# Colour

Material 3 colour roles generated with material-color-utilities from `seed-signal` (#00B8D9, "signal cyan"): the *Vibrant* scheme for Dark and Sunlight, *Monochrome* for Discreet, then a few tones overridden per theme for street contrast (listed in `tools/tokens.source.json`). Zapper's default is indigo; signal cyan sits about 50° of hue away, and blue-cyan is the colour people already read as "you are here" on a map.

## One accent, no presets, no dynamic colour

Zapper offers six seeds and wallpaper colour. Here every phone in a crowd shows the same colours, so a pin described by a friend ("the red diamond near the bridge") looks the same on your screen, and a wallpaper can't produce an accent that collides with a report family.

## Report families

Fixed, not seed-derived (like Zapper's power red). **Colour is never the only cue**: each family has its own shape.

| Family | Colour | Shape | Report types (SPEC FR-4.1) |
|---|---|---|---|
| `hazard` | red | diamond | Police line, Kettle / blocked in, Tear gas, Arrests happening |
| `blocked` | amber | rounded square | Street closed |
| `safe` | green | circle | Medic here, Water / supplies, Exit open, All clear here |

Each family has four roles: `X` (pin fill), `on-X` (pin icon), `X-container` (picker tile, detail header) and `on-X-container`.

## Group halos

`group-1` to `group-5` (cyan, blue, violet, magenta, lime) mark which of your up to five groups a report came from. A group colour is only ever a ring and a monogram badge, never a fill, so it can't be mistaken for a family.

## Role map

| Surface | Token |
|---|---|
| Map | `map-land`, `map-building`, `map-road-minor`, `map-road-major`, `map-road-casing`, `map-water`, `map-park`, `map-label` |
| Overlay chips and icon buttons on the map | `surface-container-high`, `elevation-1` |
| Report button, your position, mesh-on icon | `primary` / `on-primary` |
| Position-sharing bar | `primary-container` / `on-primary-container` |
| Group report pin | family fill, `group-N` halo and monogram |
| Public report pin | `X-container` fill, dashed `X` edge, `pin-halo` ring |
| Mesh: alone | `blocked-container` |
| Mesh: radio or permission missing, panic wipe | `error`, `error-container` |
| Sheets | `surface-container`, scrim at `scrim-opacity` |
| Captions, ages, distances | `on-surface-variant` |

## Contrast is a gate

`python3 tools/tokens.py check` computes every pair listed in `tokens.source.json → contrast` in every theme and fails on any miss: text 7:1 (WCAG AAA) in Dark and Sunlight, 4.5:1 in Discreet (deliberately dim), 3:1 for pins, halos and outlines against the map. `ContrastTest.kt` repeats the check against the Compose colours. The [Accessibility](../Accessibility/README.md) card computes the same ratios live in the gallery.
