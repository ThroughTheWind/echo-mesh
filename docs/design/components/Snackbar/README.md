# Snackbar

**Purpose:** confirm something that just happened, or give a short window to cancel it.

## Anatomy

64dp minimum, `inverse-surface`, `radius-md`, `label-medium` message, at most one text action in `inverse-primary`. Appears above the Report button and pushes nothing.

## Behaviour

- Default duration 4 s; with an action, as long as the action is meaningful (the 5 s send grace period).
- Never stacks: a new snackbar replaces the old one.
- In discreet mode it appears without motion.

**Compose:** M3 `SnackbarHost` with a custom `Snackbar` height.

## Tokens

| Token | Value |
|---|---|
| `--c-snackbar-min-height` | `64px` |
| `--c-snackbar-radius` | `{radius-md}` |
| `--c-snackbar-container` | `{inverse-surface}` |
| `--c-snackbar-content` | `{inverse-on-surface}` |
| `--c-snackbar-action` | `{inverse-primary}` |
