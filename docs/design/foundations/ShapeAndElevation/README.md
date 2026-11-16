# Shape and elevation

## Shape

Zapper's radius scale without the user-selectable key styles: pin shapes carry meaning, so they never change.

| Token | Value | Use |
|---|---|---|
| `radius-xs` | 4 | Badges, group monograms |
| `radius-sm` | 8 | `blocked` pins, chips' inner elements |
| `radius-md` | 12 | Cards, list groups, text fields, callouts |
| `radius-lg` | 16 | Buttons, picker tiles, segmented buttons |
| `radius-xl` | 28 | Bottom sheets, dialogs |
| `radius-full` | pill | Chips, the Report button, `safe` pins, avatars |

**Pin shapes:** `hazard` a 36dp square rotated 45° with 6dp corners, `blocked` a 40dp square with `radius-sm`, `safe` a 44dp circle. All sit inside the same 64dp hit area.

## Elevation

Tonal elevation (the `surface-container-*` ladder) does most of the work. Shadows exist only where something floats over the busy map:

- `elevation-1`: chips, icon buttons and the Report button on the map.
- `elevation-3`: bottom sheets and dialogs.

Nothing else casts a shadow. In Discreet the shadows stay, but they disappear against true black, which is fine: the scrim separates sheets.
