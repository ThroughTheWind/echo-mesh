# Floating action button

**Purpose:** the map's single primary action, **Report**. Echo has exactly one FAB.

## Anatomy

`touch-hero` (88dp) tall, pill-shaped, `primary` / `on-primary`, `add_location_alt` icon plus "Report" in `glance-label`. Bottom right, `space-lg` from the edges, `elevation-1`.

## Behaviour

- Opens the [report picker](../../patterns/ReportPicker/README.md).
- Collapses to an 88dp circle (icon only) while a sheet covers the lower half; hides while a full sheet is open.
- Never moves to make room for a banner: banners go under the top bar instead.

## Accessibility

Content description "Report", and the TalkBack hint "Drops a report at the map centre".

**Compose:** M3 `ExtendedFloatingActionButton` with `Modifier.height(Touch.hero)`.

## Tokens

| Token | Value |
|---|---|
| `--c-fab-height` | `{touch-hero}` |
| `--c-fab-radius` | `{radius-full}` |
| `--c-fab-container` | `{primary}` |
| `--c-fab-content` | `{on-primary}` |
| `--c-fab-label` | `{glance-label}` |
