# Spacing and layout

## Spacing

Zapper's 4dp scale, unchanged: `space-xxs` 2, `space-xs` 4, `space-sm` 8, `space-md` 12, `space-lg` 16, `space-xl` 20, `space-xxl` 24, `space-xxxl` 32, `space-huge` 48, `space-giant` 64. `space-md` (12dp) is also the **minimum gap between two touch targets**: gloves are imprecise.

## Screen anatomy

- Portrait only (SPEC NFR-13). The map is full-bleed; overlays sit on a `space-lg` (16dp) margin.
- **Top bar** (`top-bar-height`, 64dp, transparent over the map): mesh status on the left; discreet toggle and panic wipe on the right. The top of the screen is hard to reach with one hand, so it holds only status and the two actions that should be hard to hit by accident.
- **Thumb zone** (`thumb-zone`, the bottom 45%): the Report button bottom right, centre-on-me above it, the filter bottom left, the map scale above the filter.
- **Sheets** rise from the bottom; their primary action is always the lowest, so the thumb doesn't travel.
- **Bars** (position sharing, errors) sit just under the top bar and never cover the Report button.
- **Full screens** (settings, groups) use the [top app bar](../../components/TopAppBar/README.md), a single column with `space-lg` margins, and put their primary action at the bottom.

## Sizes

| Token | Value | Use |
|---|---|---|
| `touch-min` | 64dp | Smallest hit area of anything tappable, pins included (Zapper: 56) |
| `touch-comfort` | 72dp | Buttons, list rows, picker tiles |
| `touch-hero` | 88dp | The Report button, hold to wipe |
| `pin-size` / `pin-size-large` | 44 / 56dp | Pin visual size |
| `top-bar-height` | 64dp | Map top bar and top app bar |
