# Echo design system

The design system for Echo, a free, open-source, peer-to-peer protest map for Android. It is a sibling of [Zapper's design system](https://github.com/ThroughTheWind/zapper/tree/main/docs/design): same Material 3 base, same Roboto Flex, same 4dp spacing, same token files and the same Compose setup. What changes is the setting. Zapper is used on a sofa with eyes on a TV; Echo is used standing in a crowd, in sun or at night, with gloves, one hand, and sometimes people nearby who shouldn't see your screen.

> Package `io.github.throughthewind.echo`. English only. Product scope: the [MVP spec](../SPEC.md). The Compose theme lives in `core/designsystem/` (package `…echo.core.designsystem.theme`). Open [`preview/index.html`](preview/index.html) for every entry in all three themes: one self-contained file that makes no network request.

## Structure

The system has four tiers. Each lower tier is built only from the tiers above it.

| Tier | What it holds | Folder |
|---|---|---|
| **Foundations** | Principles, design tokens, and the rules for colour, type, space, shape, icons, motion, accessibility, themes and voice | [`foundations/`](foundations/) |
| **Components** | Reusable building blocks: generic ones first, then Echo's own (pin, expiry, mesh status) | [`components/`](components/) |
| **Patterns** | Components combined to get one job done (drop a report, join a group, wipe) | [`patterns/`](patterns/) |
| **Screens** | Whole screens at phone size | [`screens/`](screens/) |

Every entry is a folder with a `README.md` (the spec) and a `preview.html` (a fragment rendered into the gallery). The card header in the gallery shows each entry's status: **Ready** (settled), **Spec** (specified and previewed, not yet in Compose), **Built** (in `core/designsystem`).

## Foundations

- [Principles](foundations/Principles/README.md): the six rules every decision traces back to. Glance, then act; dark first, sunlight ready, discreet on demand; built for gloves and one hand; honest by construction; capabilities, not radios; open, native and offline.
- [Design tokens](foundations/DesignTokens/README.md): three tiers (primitive palettes, semantic roles, component tokens), naming, and how to change one.
- **Visual:** [Colour](foundations/Colour/README.md) · [Typography](foundations/Typography/README.md) · [Spacing and layout](foundations/SpacingAndLayout/README.md) · [Shape and elevation](foundations/ShapeAndElevation/README.md) · [Iconography](foundations/Iconography/README.md) · [Motion and haptics](foundations/Motion/README.md)
- **Usage:** [Accessibility](foundations/Accessibility/README.md) · [Themes and customisation](foundations/Themes/README.md) · [Content and voice](foundations/ContentAndVoice/README.md)

## Components

- **Actions:** [Button](components/Button/README.md) · [Icon button](components/IconButton/README.md) · [Floating action button](components/FloatingActionButton/README.md) · [Hold-to-confirm button](components/HoldToConfirmButton/README.md)
- **Selection:** [Chip](components/Chip/README.md) · [Segmented button](components/SegmentedButton/README.md) · [Switch](components/Switch/README.md) · [Radio button and checkbox](components/RadioAndCheckbox/README.md)
- **Inputs:** [Text field](components/TextField/README.md)
- **Containment:** [List item](components/ListItem/README.md) · [Card](components/Card/README.md) · [Bottom sheet](components/BottomSheet/README.md) · [Dialog](components/Dialog/README.md)
- **Feedback:** [Callout](components/Callout/README.md) · [Snackbar](components/Snackbar/README.md) · [Progress indicator](components/ProgressIndicator/README.md)
- **Identity:** [Avatar and badge](components/AvatarAndBadge/README.md)
- **Navigation:** [Top app bar](components/TopAppBar/README.md)
- **System:** [System notification](components/SystemNotification/README.md)
- **Echo:** [Report pin](components/ReportPin/README.md) · [Expiry indicator](components/ExpiryIndicator/README.md) · [Mesh status](components/MeshStatus/README.md)

## Patterns

- **First run and setup:** [Onboarding](patterns/OnboardingFlow/README.md) · [Permissions in context](patterns/PermissionsInContext/README.md) · [Get a map](patterns/GetAMap/README.md)
- **Mesh:** [Mesh on and off](patterns/MeshOnOff/README.md)
- **Reports:** [Drop a report](patterns/ReportPicker/README.md) · [Report detail](patterns/ReportDetail/README.md)
- **Groups:** [Create a group](patterns/CreateGroup/README.md) · [Join a group by QR](patterns/QrJoinFlow/README.md) · [Position sharing](patterns/PositionSharing/README.md) · [Mute, leave and group end](patterns/MuteLeaveAndGroupEnd/README.md)
- **Safety:** [Panic wipe](patterns/PanicWipeConfirm/README.md) · [Discreet mode](patterns/DiscreetMode/README.md)
- **States:** [Empty and error states](patterns/EmptyAndErrorStates/README.md)

## Screens

[Map](screens/MapView/README.md) (home) · [Reports near you](screens/ReportsNearYou/README.md) · [Group roster](screens/GroupRoster/README.md) · [Maps](screens/MapsScreen/README.md) · [Settings](screens/SettingsScreen/README.md) · [What Echo protects](screens/WhatEchoProtects/README.md)

## Spec coverage

[`SPEC-COVERAGE.md`](SPEC-COVERAGE.md) maps every requirement, user story and acceptance criterion in the spec to the entries that answer it, including the ones with no UI. `tools/tokens.py check` fails when the spec gains an id without a row.

## Adding or changing an entry

1. Pick the tier: a reusable piece is a component, a task built from several is a pattern, a full screen is a screen.
2. Copy [`TEMPLATE.md`](TEMPLATE.md) to `<tier>/<Name>/README.md` and fill every section; delete a section only when it truly doesn't apply.
3. Add component tokens for any new property in `tools/foundations.source.json` → `component`. Components never use a hex value or a palette tone.
4. Write `<tier>/<Name>/preview.html`, a fragment starting with `<!-- @dsCard group="…" order="…" status="Spec" subtitle="…" -->`, styled with `assets/bundle.css` and nothing else. No external requests.
5. If it adds a colour pair that carries text or meaning, add the pair to `tools/tokens.source.json` → `contrast`.
6. If it answers a spec requirement, name it in that requirement's row of [`SPEC-COVERAGE.md`](SPEC-COVERAGE.md).
7. `python3 tools/tokens.py build`, then `python3 tools/tokens.py check`. The check (contrast, token references, freshness, spec coverage, relative links) is a rungs fast-tier gate (`design-tokens`).
8. When the Compose version lands, set the status to **Built** and name the composable in the README.

## Files

| Path | What | Edited by |
|---|---|---|
| `tools/tokens.source.json` | Seed, theme tone overrides, report families, groups, map tones, contrast rules | Hand |
| `tools/foundations.source.json` | Type, spacing, radius, elevation, sizes, opacity, motion, component tokens | Hand |
| `tokens.json` | Every token, all three tiers and themes | `tools/tokens.py generate` |
| `assets/tokens.css` | CSS custom properties: `--ref-*`, semantic roles, `--c-*` | `tools/tokens.py build` |
| `assets/bundle.css` | Preview styles, built on the tokens | Hand |
| `assets/icons.svg` | Icon sprite for previews | `tools/icons.py` |
| `preview/index.html` | The gallery | `tools/tokens.py build` |
| `SPEC-COVERAGE.md` | Spec id to design entry map | Hand, checked by `tools/tokens.py check` |
| `core/designsystem/…/theme/Color.kt` | Compose colours | `tools/tokens.py build` |
| `core/designsystem/…/theme/{Theme,Type,Dimens}.kt`, `ContrastTest.kt` | Compose theme | Hand |

`tools/tokens.py generate` needs `pip install materialyoucolor` and runs only when a colour changes; `build` and `check` use the standard library only.

## Open questions for other threads

- **Threat model:** whether `FLAG_SECURE` covers every screen or only the QR (FR-12.5 says the QR; recommended: every screen, since the recents thumbnail of a map shows where you are). The design works either way.
- **Transport:** whether the peer count can be honest at crowd density (the chip caps at "20+"); which OEMs get their own battery instructions page (FR-3.3a).
- **Spec:** the 5-second send delay in ReportPicker (a sent report can't be unsent) needs an FR; the "Reports near you" list for TalkBack needs an FR; the Quick Settings tile for discreet mode needs an FR.

Settled since the first draft: the protects/doesn't wording follows threat model §7; no launcher disguise (FR-13.2); BLE only, so no Wi-Fi or location toggles in the mesh status; no word comparison in the QR join (FR-8.2).
