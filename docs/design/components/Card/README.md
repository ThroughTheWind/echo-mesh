# Card

**Purpose:** group related content that isn't a list row: a downloaded map, an explanatory entry point.

## Variants

- **Filled:** `surface-container-high`, `radius-md`, `space-lg` padding.
- **Outlined:** transparent with a 2dp `outline-variant` border, for lower emphasis.

Cards are flat (no shadow) and never nested. If a card is tappable as a whole, it has one action and a visible affordance.

**Compose:** M3 `Card` / `OutlinedCard`.

## Tokens

| Token | Value |
|---|---|
| `--c-card-radius` | `{radius-md}` |
| `--c-card-container` | `{surface-container-high}` |
| `--c-card-padding` | `{space-lg}` |
