# Echo

The design system for Echo, a free, open-source, peer-to-peer protest map for Android. It is a sibling of [Zapper's design system](https://github.com/ThroughTheWind/zapper/tree/main/docs/design): same Material 3 base, same Roboto Flex, same 4dp spacing, same token files, same Compose layout and the same one-README-and-one-preview-per-component habit. What changes is the setting. Zapper is used on a sofa with eyes on a TV. This app is used standing in a crowd, in sun or at night, with gloves, one hand, and sometimes people around you who shouldn't see your screen.

> Package `io.github.throughthewind.<appname>` (currently `protestmap` in code, until the package is renamed). English only. Product scope: the [MVP spec](../SPEC.md). The Compose implementation will live in `core/designsystem/` (package `…core.designsystem`). Open [`preview/index.html`](preview/index.html) to see every component in all three themes; it is one self-contained file and makes no network request.

## Principles

1. **Glance, then act.** The map must answer three questions in under a second at arm's length: what's dangerous near me, where's help, and can my phone hear anyone. Pins are big, families are told apart by shape *and* colour, the numbers that matter are 18sp or larger, and text targets 7:1 contrast.
2. **Dark first, sunlight ready, discreet on demand.** `dark` is the default: near-black surfaces save OLED battery and don't light up a face at night. `sun` is a high-contrast white theme for direct sunlight. `discreet` is black and grey, dimmed, silent and still. See [Themes](#themes).
3. **Built for gloves and one hand.** Every target is at least `touch-min` (64dp, Zapper uses 56), with `space-md` (12dp) between targets. Frequent actions live in the bottom `thumb-zone` (45% of the screen): the Report button, centre-on-me, the filter, every sheet's buttons.
4. **Honest by construction.** The UI never claims more safety than the threat model supports. Verified (group) and unverified (public) reports look different everywhere. A public count says "4 nearby phones confirmed", never "verified". Copy that makes a safety claim cites a line in the threat model (SPEC NFR-14).
5. **Capabilities, not radios.** Like Zapper's "brand-agnostic" rule: components show what the mesh can do ("Short range, low power", "Works with the screen off"), never "BLE" or "Wi-Fi Direct". A radio's own name appears only where the user must switch it on in Android settings, and that string comes from the transport adapter.
6. **Open, native and offline.** Material Symbols Rounded (Apache-2.0) and Roboto Flex (OFL-1.1) are **bundled** in the APK. Zapper loads Roboto Flex as a downloadable font, which goes through Google Play Services and the network; this app may not (constraints 2 and 3). No dynamic colour either: see [Colour](#colour).

## Themes

| Theme | When | What changes |
|---|---|---|
| **Dark** (default) | Night, shade, most of the time | Near-black canvas (neutral tone 4), signal-cyan accent, light report pins. |
| **Sunlight** | Direct sun | Pure white canvas, near-black text, darker accent and outlines, +100 font weight, road casings on the map. |
| **Discreet** | You don't want your screen noticed | True black, grey content around tone 60 to 75, no accent hue, report colours desaturated (chroma ≤ 18). The window's brightness drops to `discreet-brightness` (0.12), haptics turn off, the foreground notification uses neutral text and icon. Motion is reduced to state changes only. |

Settings > Display > Theme: **Auto** (dark; sunlight when the ambient light sensor reads bright outdoor light, with hysteresis so it doesn't flicker), **Dark**, **Sunlight**. Discreet mode is not in that list: it is a one-tap toggle on the map's top bar and a Quick Settings tile, and it overrides the theme until turned off.

**Discreet is not disguise.** It makes the screen dim, still and silent so it doesn't draw a glance. It does not hide the app from someone holding the phone. Whether to also offer a disguised launcher name and icon is open (SPEC FR-13.2): the threat model decides, since a disguise can look worse when found. Nothing here depends on that choice.

## Colour

Material 3 roles generated with material-color-utilities from `seed-signal` (#00B8D9), *Vibrant* scheme for dark and sunlight and *Monochrome* for discreet, then a few tones overridden per theme for street contrast (listed in `tools/tokens.source.json`). Zapper's default is indigo; signal cyan sits about 50° of hue away, and blue-cyan is the colour people already read as "you are here" on a map.

**One accent, no presets, no dynamic colour.** Zapper offers six seeds and wallpaper colour. Here every phone in a crowd shows the same colours, so a pin described by a friend ("the red diamond near the bridge") looks the same on your screen, and a wallpaper can't produce an accent that collides with a report family.

Fixed families, not seed-derived (like Zapper's power red). **Colour is never the only cue**: each family has its own shape.

| Family | Colour | Shape | Report types (SPEC FR-4.1) |
|---|---|---|---|
| `hazard` | red | diamond | Police line, Kettle / blocked in, Tear gas, Arrests happening |
| `blocked` | amber | rounded square | Street closed |
| `safe` | green | circle | Medic here, Water / supplies, Exit open, All clear here |

Group halos `group-1` to `group-5` (cyan, blue, violet, magenta, lime) mark which of your up-to-five groups a report came from. A group colour is only ever a ring and a monogram badge, never a fill, so it can't be mistaken for a family.

How roles map to the app:

| Surface | Token |
|---|---|
| Map | `map-land`, `map-building`, `map-road-minor`, `map-road-major`, `map-road-casing`, `map-water`, `map-park`, `map-label` |
| Overlay chips and icon buttons on the map | `surface-container-high`, `elevation-1` |
| Report button, your position, mesh-on icon | `primary` / `on-primary` |
| Position-sharing bar | `primary-container` / `on-primary-container` |
| Group report pin | family fill, `group-N` halo and monogram |
| Public report pin | `<family>-container` fill, dashed `<family>` edge, `pin-halo` ring |
| Report picker tile, detail header | `<family>-container` / `on-<family>-container` |
| Mesh: alone | `blocked-container` |
| Mesh: radio or permission missing, panic wipe | `error`, `error-container` |
| Sheets | `surface-container`, scrim at `scrim-opacity` (0.50) |
| Captions, ages, distances | `on-surface-variant` |

**Contrast is a gate, not a promise.** `python3 tools/tokens.py check` computes every pair in `tokens.source.json → contrast` in every theme and fails on any miss: text 7:1 (AAA) in dark and sunlight, 4.5:1 in discreet (deliberately dim), 3:1 for pins, halos and outlines against the map. `ContrastTest.kt` repeats the check against the Compose colours, as in Zapper.

## Typography

Roboto Flex, bundled. Zapper's M3 scale shifted up one step, plus three **glance** styles. Nothing is smaller than 13sp; no sentence is smaller than 16sp.

- `glance-numeral` 40/44 700 tabular: the one number on a screen that matters (hold-to-wipe countdown).
- `glance-title` 24/30 700: a report's type in its detail sheet.
- `glance-label` 18/22 700: picker tiles, big buttons, the Report button.
- `label-small` 13/16 700: pin badges and the map scale. The floor.

Sunlight adds `sunWeightBoost` (+100) to every weight: thin strokes are the first thing glare erases. Font scaling: sheet and settings text scales freely; pin badges cap at 1.3×; pins themselves follow Settings > Display > Large pins (`pin-size-large`) rather than font scale.

## Layout

- Portrait only (SPEC NFR-13). The map is full-bleed; overlays sit on a 16dp margin.
- **Top bar** (64dp, transparent over the map): mesh status chip on the left, then discreet toggle and panic wipe on the right. Nothing else: the top of the screen is hard to reach with one hand, so it only holds status and the two actions that should be hard to hit by accident.
- **Thumb zone** (bottom 45%): Report (`touch-hero`, 88dp) bottom right, centre-on-me above it, filter bottom left, map scale above the filter.
- **Sheets** rise from the bottom; their primary action is always the lowest one, so the thumb doesn't travel.
- **Bars** (position sharing, errors) sit just under the top bar and never cover the Report button.

## Shape

Zapper's radius scale without the user-selectable key styles: pin shapes carry meaning, so they can't change. Pins: `hazard` diamond (36dp square rotated 45°, 6dp corners), `blocked` 40dp square with `radius-sm`, `safe` 44dp circle. Buttons and tiles use `radius-lg`, sheets `radius-xl`, chips and the Report button `radius-full`.

## Iconography

Material Symbols **Rounded**, filled, weight 500, bundled as vector drawables (only the glyphs used). `icon-pin` (24) in pins, `icon-md` (28) in buttons, `icon-lg` (36) where a glyph must read at arm's length. One glyph per concept everywhere: police line `local_police`, kettle `fence`, tear gas `foggy`, arrests `person_alert`, street closed `block`, medic `medical_services`, water `water_drop`, exit `door_open`, all clear `check_circle`, mesh `radar`, wipe `delete_forever`, discreet `visibility_off`. Icons are never the only label: every pin and button has a content description ("Tear gas, from Crew, 11 minutes left, 4 confirmations").

## Motion and haptics

See [motion-and-haptics.md](motion-and-haptics.md). In short: fast and functional, nothing decorative; a new pin pops once; hold-to-wipe ticks every 0.5s; discreet mode turns all haptics off and reduces motion.

## Accessibility

- 64dp targets, 12dp gaps. Colour is never the only signal (shape per family, dashed edge for public, monogram for group).
- WCAG AAA text contrast in dark and sunlight, AA in discreet, 3:1 for every non-text mark; gated.
- TalkBack: pins are buttons whose label says type, source, time left and confirmations. The map offers a "Reports near you" list (sorted by distance) so dropping and reading a report never needs the map gesture (SPEC NFR-12).
- Every gesture has a tap alternative, except hold-to-wipe, which is deliberate: TalkBack users get a "Wipe" custom action that asks for a double-tap-and-hold, the platform's own long-press.
- Hardware keyboard and switch access: 3px `secondary` focus ring.

## Components

**Map:** MapView. **Reports:** ReportPin, ReportPicker, ReportDetail, ExpiryIndicator. **Mesh:** MeshStatus. **Groups:** GroupRoster, QrJoinFlow. **Safety:** PanicWipeConfirm. **Onboarding:** OnboardingFlow. Plus a Foundations card in the gallery. Each has a README and a `preview.html` fragment under [`components/`](components/).

Shared with Zapper and not redrawn here (reuse Zapper's README, restyled by these tokens): Banner and snackbar, Dialog, TextField, SettingsControls.

## Voice

English only. Short, plain, second person. Say what's true and stop: "Anyone running the app can send these, and it could be wrong or fake." Never "secure", "anonymous", "safe" or "verified" as an absolute; say what is protected and from whom. Name report types in the words people shout ("Tear gas", "Exit open"). Destructive buttons use the verb ("Wipe", "Leave Crew", "Withdraw report").

## Files

- `tokens.json`: every token, all three themes. **Generated**; never edit by hand.
- `tools/tokens.source.json`, `tools/foundations.source.json`: the hand-edited sources (seed, theme overrides, families, groups, map tones, contrast rules, type, sizes).
- `tools/tokens.py`:
  - `generate` turns the sources into `tokens.json` (needs `pip install materialyoucolor`; run only when a colour changes).
  - `build` writes `components/tokens.css`, the Compose `Color.kt` and the gallery `preview/index.html` (standard library only).
  - `check` fails on any contrast miss or any stale or hand-edited generated file. It needs only the standard library, so it can be a rungs fast-tier gate.
- `tools/icons.py`: builds `components/icons.svg` from the Material Symbols package.
- `components/bundle.css`: the shared preview styles, mirroring the Compose components.
- Compose drafts (to move into `core/designsystem/` when the app repo exists): `theme/Color.kt` (generated), `Theme.kt` (`StreetTheme`, theme resolution, discreet brightness), `Type.kt`, `Dimens.kt`, and `ContrastTest.kt`.
- [theming-and-customisation.md](theming-and-customisation.md), [motion-and-haptics.md](motion-and-haptics.md).

## Open questions for other threads

- **Threat model:** final wording of "what it protects / doesn't" (OnboardingFlow, MeshStatus sheet, ReportDetail provenance); whether discreet mode adds a launcher disguise; whether `FLAG_SECURE` covers every screen or only the QR (the design works either way; recommended: every screen, since the recents thumbnail of a map shows where you are).
- **Transport:** whether the peer count can be honest at crowd density (the chip caps at "20+"); which capability names the mesh sheet lists.
- **Spec:** the 5-second send delay in ReportPicker (a sent report can't be unsent) needs an FR; "Reports near you" list for TalkBack needs an FR.
