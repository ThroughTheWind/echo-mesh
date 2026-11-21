# MapView

The home screen: the offline map with reports, your position, group members who share theirs, the mesh status and the Report button (SPEC F2, F3, F4, F10).

**Compose:** `StreetMap(state, onPinClick, onReport, …)` wrapping the renderer chosen in `:core:map`. The design system owns the map *style* (`map-*` tokens) and the overlays; the renderer draws tiles.

## Anatomy

- **Map:** full-bleed, styled with `map-land`, `map-building`, `map-road-minor/major`, `map-road-casing` (sunlight only), `map-water`, `map-park`, `map-label`. Street labels at `label-small` minimum. Muted on purpose so pins carry the colour.
- **Top bar** (no background): groups, settings, discreet toggle and panic wipe in the top right corner, as filled icon buttons (64dp); the MeshStatus chip on its own line under them, left. Four 64dp buttons leave a phone no room for the chip on the same line, and on one line the panic wipe was pushed off the screen. What Echo protects is under Settings > Privacy (FR-1.3).
- **Position-sharing bar** (when sharing, SPEC FR-10.4): `primary-container`, under the top bar: "Sharing your position with **Crew** · 1 h 42 left · Stop". Stop is one tap, no confirm.
- **Pins:** ReportPin, sorted so hazards draw above safe pins and group above public. Overlapping pins cluster into a count bubble below zoom 16; tapping it zooms in.
- **You:** `primary` 22dp dot with a 4dp `surface` ring and a 2dp `primary` ring; a heading cone when the compass is reliable. Shown on your own screen only; never sent unless you share.
- **Group members** (sharing only): avatar with initials and their group's halo, age underneath ("40s"). Over 2 minutes old: dashed `outline` ring and 70% opacity; over the position TTL: removed. Kept in memory only (FR-10.3).
- **Thumb zone:** Report (`touch-hero`, `primary`, label + icon) bottom right; centre-on-me above it; filter (everything / my groups only, FR-9.3) bottom left with the scale bar above.

## States

- **No map loaded:** blank `map-land` with a faint 100m grid and a banner "No map here · Get a map" (pins still show, positioned on the grid).
- **No phones nearby** (mesh on, nobody in range after the first 30 s): an amber banner under the top bar, "No phones nearby. Reports can't reach you, so an empty map doesn't mean all clear." (extended spec FR-16.2). It goes as soon as a phone is in range.
- **Mesh off:** pins you already have stay until they expire; the chip says "Mesh off · Turn on".
- **Discreet:** discreet theme, brightness `discreet-brightness`, Large pins ignored, cone hidden.
- **Filter on (groups only):** filter button shows the `group` glyph; public pins hide.

## Rules

- Never auto-centre on the user while they pan; the centre-on-me button does it.
- Never show a pin's sender, only its source class (a named group, or "nearby").
- TalkBack: the map is one element with a "Reports near you" action that opens a list sorted by distance; each row is a ReportDetail entry point.
