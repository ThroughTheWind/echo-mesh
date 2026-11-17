# Top app bar

**Purpose:** title and way back on full screens (settings, groups, join flow). The map screen has its own transparent overlay bar (see [Map view](../../screens/MapView/README.md)).

## Anatomy

64dp, `surface`, leading Back (or Close for a flow that cancels), title in `title-large`, at most one trailing icon button (overflow). No search, no tabs: Echo's navigation is the map plus a few full screens one level deep.

## Navigation model

Map (home) → full screens (Settings, Groups, Join) → at most one more level (a group's roster). Back always returns toward the map. There is no bottom navigation bar: it would sit in the thumb zone the Report button needs.

**Compose:** M3 `TopAppBar` with `Modifier.height(TopBar.height)`.

## Tokens

| Token | Value |
|---|---|
| `--c-top-app-bar-height` | `{top-bar-height}` |
| `--c-top-app-bar-container` | `{surface}` |
| `--c-top-app-bar-title` | `{title-large}` |
