# Maps screen

**Purpose:** manage offline maps (SPEC FR-2.2, FR-2.4, FR-2.6). Reached from Settings > Maps, onboarding, and the no-map empty state.

## Structure

- [Top app bar](../../components/TopAppBar/README.md) "Maps".
- One [list item](../../components/ListItem/README.md) per map: name, size, when loaded; overflow opens a sheet with **Share file** ([Get a map](../../patterns/GetAMap/README.md), step 4) and **Delete** (frees the space, no confirm needed beyond the sheet, since maps are re-downloadable).
- A switch row **Wipe also deletes maps**, off by default (FR-2.6): "Off: maps are public, but they show which city you care about."
- Bottom actions: **Download a city** (tonal), **Load a map file** (outlined).

## States

No maps: the empty state from [Empty and error states](../../patterns/EmptyAndErrorStates/README.md). Importing: a row with a progress bar in place of the size.
