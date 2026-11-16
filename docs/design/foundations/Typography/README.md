# Typography

Roboto Flex (OFL-1.1), **bundled** in the APK as a font resource. Never the downloadable-fonts provider: it goes through Google Play Services and the network.

## Scale

Zapper's Material 3 scale shifted up one step, plus three **glance** styles for the things read at arm's length. Values are in [`tokens.json`](../../tokens.json) → `type`.

| Style | Size / line | Weight | Use |
|---|---|---|---|
| `glance-numeral` | 40/44 | 700, tabular | The one number that matters on a screen (hold-to-wipe countdown) |
| `glance-title` | 24/30 | 700 | A report's type in its detail sheet |
| `glance-label` | 18/22 | 700 | Picker tiles, the Report button, big buttons |
| `headline-medium` | 28/36 | 600 | Onboarding titles |
| `headline-small` | 24/32 | 600 | Sheet and dialog titles for important flows |
| `title-large` | 22/28 | 600 | Top app bar, group name |
| `title-medium` | 18/24 | 600 | Sheet titles, list headlines |
| `body-large` | 18/26 | 400 | Onboarding and honesty copy |
| `body-medium` | 16/24 | 400 | Default body; the smallest size for a sentence |
| `label-large` | 16/20 | 700 | Buttons |
| `label-medium` | 14/18 | 700 | Chips, list support lines, status |
| `label-small` | 13/16 | 700 | Pin badges, map scale; the floor |

## Rules

- Nothing is smaller than 13sp; no sentence is smaller than 16sp.
- Sunlight adds `sunWeightBoost` (+100) to every weight: glare erases thin strokes first.
- Numbers that change (counts, times left) use tabular figures so they don't jitter.
- Font scaling: sheet, list and settings text scales freely; pin badges cap at 1.3×; pins follow Settings > Display > Large pins rather than font scale.
- Sentence case everywhere, including buttons. No all-caps labels except the gallery's own captions.

**Compose:** `streetTypography(sun)` and `LocalGlanceType` in `theme/Type.kt`.
