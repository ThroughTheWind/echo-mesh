# Bottom sheet

**Purpose:** every task started from the map (report, detail, mesh, wipe) happens in a bottom sheet, so the map stays in view and the controls stay in the thumb zone.

## Anatomy

`surface-container`, `radius-xl` top corners, `elevation-3`, drag handle, title (`title-medium` or `headline-small` for important flows) with an optional close button, content, then actions with the primary lowest.

## Behaviour

- Opens at half height (detail) or content height (picker, wipe); drag up to expand when content scrolls.
- Modal sheets dim the map with `scrim` at `scrim-opacity` (0.50); tapping the scrim or dragging down dismisses.
- The report picker is the exception: a light scrim, because the user positions the crosshair on the map while it's open.

## Accessibility

Focus moves into the sheet when it opens and returns to the element that opened it; Back closes it.

**Compose:** M3 `ModalBottomSheet`.

## Tokens

| Token | Value |
|---|---|
| `--c-sheet-container` | `{surface-container}` |
| `--c-sheet-radius` | `{radius-xl}` |
| `--c-sheet-scrim` | `{scrim}` |
| `--c-sheet-scrim-opacity` | `{scrim-opacity}` |
| `--c-sheet-shadow` | `{elevation-3}` |
