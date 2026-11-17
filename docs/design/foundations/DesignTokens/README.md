# Design tokens

Every visual value in Echo is a token in [`tokens.json`](../../tokens.json), generated from two hand-edited sources by [`tools/tokens.py`](../../tools/tokens.py). Tokens come in three tiers, the usual structure for a design system:

| Tier | What | Examples | Changes per theme |
|---|---|---|---|
| **1 · Primitive** | Tonal palettes, tones 0 to 100 (HCT): `accent`, `accent-secondary`, `neutral`, `neutral-variant`, `error`, `grey`, `hazard`, `blocked`, `safe`, `group-1`…`group-5` | `--ref-hazard-40` | No |
| **2 · Semantic** | Roles with a meaning: Material 3 colour roles, report families, group halos, map style; plus type, spacing, radius, elevation, size, opacity and motion | `--primary`, `--hazard-container`, `--touch-min`, `--title-medium` | Colours yes, the rest no |
| **3 · Component** | One component's properties, each a reference to a tier 2 token or a literal | `--c-button-height: var(--touch-comfort)` | Only through what they reference |

## Rules

- **Components read tier 3 only.** A component never names a palette tone or a hex value, and should not reach for a tier 2 colour its tokens don't list. Adding a property means adding a component token.
- **Tier 2 is where themes happen.** Dark, Sunlight and Discreet pick different primitives for the same role; nothing below changes.
- **Primitives are documentation and generator input.** The app doesn't expose them, so nobody can bypass a theme.
- **Generated files are never edited by hand:** `tokens.json`, `assets/tokens.css`, `Color.kt`, `preview/index.html`. `python3 tools/tokens.py check` (a rungs fast-tier gate) fails on a stale or hand-edited copy, on any contrast pair below its minimum, and on a component token that refers to a token that doesn't exist.

## Changing a token

1. Edit `tools/tokens.source.json` (colour) or `tools/foundations.source.json` (everything else, including component tokens).
2. `python3 tools/tokens.py generate` (needs `pip install materialyoucolor`; only colour changes need it), then `python3 tools/tokens.py build`.
3. `python3 tools/tokens.py check`.

## Naming

`kebab-case`. Semantic colours follow Material 3 (`on-X` is content on `X`, `X-container` is a quieter fill). Component tokens are `<component>-<property>[-<state>]`, e.g. `button-danger-tonal-container`. Sizes are named by role, not value (`touch-min`, not `size-64`).
