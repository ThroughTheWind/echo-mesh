# Icon button

**Purpose:** an action whose icon is universally understood, where space is tight: map overlays, bars, sheet headers.

## Variants

- **Standard:** transparent, `on-surface` icon. In top app bars and sheet headers.
- **Filled:** `surface-container-high` with `elevation-1`. Anything floating over the map.
- **Wipe:** filled, with the icon in `error`. Only the panic wipe uses it.

## Anatomy

64dp circle (`touch-min`), `icon-md` (28dp) glyph, optional [badge](../AvatarAndBadge/README.md) at top right.

## Accessibility

Every icon button has a content description naming the action ("Centre on me", "Panic wipe"), and a tooltip on long-press for sighted users who don't recognise the glyph.

**Compose:** M3 `IconButton` / `FilledTonalIconButton` with `Modifier.size(Touch.min)`.

## Tokens

| Token | Value |
|---|---|
| `--c-icon-button-size` | `{touch-min}` |
| `--c-icon-button-icon` | `{icon-md}` |
| `--c-icon-button-filled-container` | `{surface-container-high}` |
| `--c-icon-button-content` | `{on-surface}` |
