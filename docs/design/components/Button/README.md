# Button

**Purpose:** start an action. One filled button per screen or sheet: the thing most people came to do.

## Variants

| Variant | Use | Colours |
|---|---|---|
| Filled | The primary action ("Send to Crew", "Open the map") | `primary` / `on-primary` |
| Tonal | A common but secondary action ("Still there", "Create a group") | `secondary-container` |
| Outlined | The alternative to a primary ("Gone", "Send to everyone nearby instead") | transparent, 2dp `outline` |
| Text | Low-emphasis escapes ("Skip for now", "Cancel") | `primary` text |
| Danger tonal | Destructive but recoverable ("Withdraw report") | `error-container` |
| Danger | Destructive and final, outside the panic wipe ("Leave group") | `error` / `on-error` |

For the panic wipe, use the [Hold-to-confirm button](../HoldToConfirmButton/README.md), never a Danger button.

## Anatomy

Optional leading icon (`icon-md`), label (`label-large`, sentence case, a verb). Height `touch-comfort` (72dp). Full width in sheets and screens; side by side only in pairs of equal width.

## States

Enabled, pressed (`state-pressed` layer), focused (3px `secondary` ring), disabled (`disabled-content` opacity, still announces why when tapped where possible).

## Accessibility

Real buttons with their visible label as the accessible name. Never icon-only: use an Icon button for that.

## Do and don't

- Do put the filled button lowest in a sheet, in the thumb zone.
- Don't use two filled buttons together; don't label a destructive action "OK".

**Compose:** M3 `Button`, `FilledTonalButton`, `OutlinedButton`, `TextButton` with `Modifier.heightIn(min = Touch.comfort)`; danger variants pass `ButtonDefaults.buttonColors(containerColor = colorScheme.error…)`.

## Tokens

| Token | Value |
|---|---|
| `--c-button-height` | `{touch-comfort}` |
| `--c-button-radius` | `{radius-lg}` |
| `--c-button-label` | `{label-large}` |
| `--c-button-gap` | `{space-md}` |
| `--c-button-primary-container` | `{primary}` |
| `--c-button-primary-content` | `{on-primary}` |
| `--c-button-tonal-container` | `{secondary-container}` |
| `--c-button-tonal-content` | `{on-secondary-container}` |
| `--c-button-outlined-border` | `{outline}` |
| `--c-button-outlined-border-width` | `2px` |
| `--c-button-danger-container` | `{error}` |
| `--c-button-danger-content` | `{on-error}` |
| `--c-button-danger-tonal-container` | `{error-container}` |
| `--c-button-danger-tonal-content` | `{on-error-container}` |
| `--c-button-text-content` | `{primary}` |
