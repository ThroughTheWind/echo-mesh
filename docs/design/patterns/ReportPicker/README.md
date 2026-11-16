# ReportPicker

Drop a report in two taps: Report, then a type; confirm with the audience named on the button (SPEC F4, FR-4.2, FR-4.3).

**Compose:** `ReportPickerSheet(types, audience, onPick, onSend)`.

## Flow

1. **Report** (FAB) opens the sheet and shows a crosshair at the map's centre, shifted up so it stays above the sheet. The report goes **at the crosshair, never at your GPS position** (FR-4.2, US-8). Pan the map to move it.
2. **Pick a type:** 3 × 3 grid of tiles (`touch-comfort` min height, 96dp in practice), grouped by family (hazards first row, then blocked, then safe). Each tile: family glyph in its shape + `label-medium` name on `<family>-container`. Below the grid, the audience toggle: **Everyone nearby** / **{Group} only**, defaulting to the last choice (FR-4.3). With several groups, the toggle's second segment opens a group list.
3. **Confirm:** the sheet shrinks to the chosen type (`glance-title`), its lifetime, and one hero button that names the audience: "Send to Crew" (lock icon) or "Send to everyone nearby". A secondary outlined button switches audience. The pin appears at the crosshair with the `new` pop.
4. **Grace period:** a snackbar "Tear gas sending to Crew in 5 s · Cancel". A report that has left the phone can't be unsent (only withdrawn, which spreads separately), so the cancel window is before sending, and the copy says "sending", not "sent". Then the `ReportSent` haptic.

## Rules

- Two taps when the crosshair is already right: Report, type… then Send. (The SPEC counts "pick, confirm"; the grace period needs no tap.)
- Rate limit hit (FR-4.6): the tile shows "Sent 40 s ago" and is disabled for the rest of the minute.
- Tiles never scroll; nine fit on a 360dp phone. More types later means a second page, not smaller tiles.
- TalkBack: tiles are buttons named by type; the crosshair is announced as "Report will be placed at the map centre, {street} area".
