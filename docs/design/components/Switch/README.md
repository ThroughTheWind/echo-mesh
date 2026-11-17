# Switch

**Purpose:** turn a setting on or off with immediate effect. If the change needs a confirm step, it's not a switch.

## Anatomy

60 × 36dp track, 24dp handle; on = `primary` track with `on-primary` handle; off = `surface-container-highest` track with a 2dp `outline` and an `outline` handle. Always the trailing element of a [list item](../ListItem/README.md); the whole row is the target.

## States

On, off, disabled (row at `disabled-content`, support line says why: "Off while discreet mode is on").

**Compose:** M3 `Switch` inside a `ListItem` with `Modifier.toggleable` on the row.

## Tokens

| Token | Value |
|---|---|
| `--c-switch-width` | `60px` |
| `--c-switch-height` | `36px` |
| `--c-switch-track-on` | `{primary}` |
| `--c-switch-handle-on` | `{on-primary}` |
| `--c-switch-track-off` | `{surface-container-highest}` |
| `--c-switch-handle-off` | `{outline}` |
