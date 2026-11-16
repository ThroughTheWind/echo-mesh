# Dialog

**Purpose:** a decision that must interrupt, on a full screen (settings, groups). On the map, use a [bottom sheet](../BottomSheet/README.md) instead: it keeps the thumb zone.

## Anatomy

312dp wide, `surface-container-high`, `radius-xl`, title in `headline-small` as a question, one or two sentences on the consequence, then actions right-aligned: the dismissive action first, the confirming action (named with its verb) last.

## Rules

- Never for the panic wipe: the hold is the confirmation, and a dialog would cost seconds.
- Never stack dialogs, never put a form in one.

**Compose:** M3 `AlertDialog`.

## Tokens

| Token | Value |
|---|---|
| `--c-dialog-container` | `{surface-container-high}` |
| `--c-dialog-radius` | `{radius-xl}` |
| `--c-dialog-width` | `312px` |
| `--c-dialog-title` | `{headline-small}` |
