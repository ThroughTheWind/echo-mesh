# List item

**Purpose:** a row in a list: settings, members, maps, reports near you.

## Anatomy

72dp minimum height (`touch-comfort`), `surface-container-high` background, 2dp gaps between rows (rows are grouped in a `radius-md` container). Leading element (icon, avatar or pin), headline (`title-medium`), optional support line (`label-medium`, `on-surface-variant`), trailing element (switch, icon button, compact time left).

## Rules

- The whole row is one target for its main action; a trailing icon button is a separate target with its own label.
- Support lines are one line; truncate with an ellipsis, never wrap past two.

**Compose:** M3 `ListItem` with `Modifier.heightIn(min = Touch.comfort)`.

## Tokens

| Token | Value |
|---|---|
| `--c-list-item-min-height` | `{touch-comfort}` |
| `--c-list-item-container` | `{surface-container-high}` |
| `--c-list-item-headline` | `{title-medium}` |
| `--c-list-item-support` | `{on-surface-variant}` |
