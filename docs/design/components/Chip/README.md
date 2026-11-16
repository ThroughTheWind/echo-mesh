# Chip

**Purpose:** compact filters, single choices in a row, and status (the [mesh status](../MeshStatus/README.md) is a specialised chip).

## Variants

- **Filter:** toggles a view ("My groups only"). Selected uses `secondary-container`.
- **Choice (outlined):** picks one of a few short values in a row, e.g. sharing duration (30 min, 1 h, 2 h).
- **Status:** read-only state with a leading icon; tappable for details.

## Anatomy

48dp visual height, pill, leading `icon-sm`, `label-medium`. The hit area extends to 64dp vertically.

## Rules

- At most four chips in a row; more choices belong in a list of [radio buttons](../RadioAndCheckbox/README.md).
- Chips never wrap their label.

**Compose:** M3 `FilterChip` / `AssistChip` with a `minimumInteractiveComponentSize` override to 64dp.

## Tokens

| Token | Value |
|---|---|
| `--c-chip-height` | `48px` |
| `--c-chip-hit` | `{touch-min}` |
| `--c-chip-radius` | `{radius-full}` |
| `--c-chip-container` | `{surface-container-high}` |
| `--c-chip-content` | `{on-surface}` |
| `--c-chip-selected-container` | `{secondary-container}` |
| `--c-chip-selected-content` | `{on-secondary-container}` |
| `--c-chip-label` | `{label-medium}` |
