# Progress indicator

**Purpose:** show that something is happening and, when known, how far along it is.

## Variants

- **Linear determinate:** map import and download. 8dp track (`surface-container-highest`), `primary` indicator, label with a percentage above.
- **Linear indeterminate:** "Looking for phones" in the mesh sheet. Replaced by a static bar under reduced motion and in discreet mode.
- **Circular:** inside a button while it works.

The [expiry indicator](../ExpiryIndicator/README.md) and the [hold-to-confirm button](../HoldToConfirmButton/README.md) are specialised progress indicators with their own rules.

**Compose:** M3 `LinearProgressIndicator` / `CircularProgressIndicator`.

## Tokens

| Token | Value |
|---|---|
| `--c-progress-height` | `8px` |
| `--c-progress-track` | `{surface-container-highest}` |
| `--c-progress-indicator` | `{primary}` |
