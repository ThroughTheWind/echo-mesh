# Radio button and checkbox

**Purpose:** choose one (radio) or any number (checkbox) of options listed vertically. Use when there are more than three options or the labels are long; otherwise use a [segmented button](../SegmentedButton/README.md) or [chips](../Chip/README.md).

## Anatomy

28dp control, 3dp `outline` ring when off; `primary` when on. Label in `body-medium` to the right. The whole 64dp row is the target.

**Compose:** M3 `RadioButton` / `Checkbox` inside a row with `Modifier.selectable` / `toggleable` and `Role.RadioButton` / `Role.Checkbox`.

## Tokens

| Token | Value |
|---|---|
| `--c-selection-size` | `28px` |
| `--c-selection-hit` | `{touch-min}` |
| `--c-selection-on` | `{primary}` |
| `--c-selection-on-content` | `{on-primary}` |
| `--c-selection-off` | `{outline}` |
