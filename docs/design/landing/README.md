**Authoritative for:** how Echo's landing page looks and behaves, and the rules that keep it honest and quiet.
**Not authoritative for:** what it says and to whom ([MESSAGING.md](MESSAGING.md)), or anything about the app itself (the [design system](../README.md)).

# Landing page design system

A thin web layer on top of the app's street design system. It adds no colour and no font: every colour is a semantic role from [`../assets/tokens.css`](../assets/tokens.css), the pins are the app's [Report pin](../components/ReportPin/README.md), and the type is the app's scale, with the glance styles as the page's loud voice. Someone who sees the page and then opens Echo should feel they're in the same place.

Open [`index.html`](index.html) in a browser for the reference page. It is a working page, not a mock-up: it uses only this system, and `tools/landing.py check` keeps it honest.

| File | What | Edited by |
|---|---|---|
| [`MESSAGING.md`](MESSAGING.md) | Positioning, audiences, message layers, words, launch and preview, objections | Hand |
| `landing.source.json` | Launch release, releases, CTA, claim register, contrast pairs, banned words | Hand |
| `landing.css` | Web tokens (`--l-*`) and components | Hand, checked |
| `theme.css` | The app's sun theme under `prefers-color-scheme: light` | `tools/landing.py build` |
| `index.html` | Reference page | Hand, except what `build` writes: claims, release line, CTA, preview ribbon, icon sprite |

```bash
python3 tools/landing.py build            # from docs/design/: write claims, release line, ribbon and sprite into index.html, and theme.css
python3 tools/landing.py check            # the landing-page gate (fast tier)
python3 tools/landing.py check --deploy   # before publishing: also fails until the launch release (1.0) is out
```

## 1. Principles

The app's six [principles](../foundations/Principles/README.md) hold. The page adds five, in priority order:

1. **Understood in five seconds, trusted in five minutes.** The hero says what Echo is and the one structural difference. Everything a sceptic needs is further down, never hidden behind a click or a script.
2. **Every promise wears its status.** A feature or protection line on the page is a claim from the register, with a computed chip: Available, Coming in 1.x, In development, Planned for 1.x, Checked by the build, Known limit, Not planned. The release line and the preview ribbon are computed too. Nobody types a status.

The page is published with Echo 1.0 and not before. Until then it is a preview of launch day ([MESSAGING.md](MESSAGING.md) §6).
3. **The limits get the same size as the strengths.** "It protects" and "It doesn't protect" are two equal cards, side by side, same type, same weight. Green check and amber warning, never a red cross (as in the app).
4. **The site behaves like the app.** No script, no cookies, no form, nothing loaded from another site, no referrer. Fonts, icons and CSS are served with the page.
5. **Calm.** No photos of crowds or faces, no alarm colours outside a report pin, no countdowns, no motion beyond a smooth scroll that `prefers-reduced-motion` turns off.

## 2. Tokens

Three tiers, as in the app ([Design tokens](../foundations/DesignTokens/README.md)). The page uses tiers 2 and 3 from `tokens.css` directly and adds a small tier 3 of its own, `--l-*`, in `landing.css`. `landing.css` may not contain a hex, `rgb()`, `hsl()` or `--ref-*` palette tone, or a font size under 13px; the check refuses them.

### Colour and themes

| Web | App theme | How |
|---|---|---|
| Default | `dark` | `tokens.css` `:root` |
| `prefers-color-scheme: light` | `sun` (and its +100 weight boost) | `theme.css`, generated from `tokens.json` |
| — | `discreet` | Not used: it's a mode for a phone in a crowd, not for reading a page |

The page has no theme switch, because a switch needs a script; it follows the reader's system setting.

| Role on the page | Token |
|---|---|
| Page | `surface` |
| Tinted section (alternate) | `surface-container-low` |
| Cards, FAQ rows, legend tiles | `surface-container-high` |
| Footer | `surface-container-lowest` (dark), `surface-container` (sun) |
| Body text / secondary text | `on-surface` / `on-surface-variant` |
| Accent: eyebrow, primary button, links on the page, focus | `primary`, `secondary` (focus ring, as in the app) |
| Links on cards | `on-surface`, underlined (`primary` falls under 7:1 on cards in sun) |
| Preview ribbon (before the launch only) | `inverse-surface` / `inverse-on-surface` |
| Report pins and the map | `hazard`, `blocked`, `safe`, `map-*`, exactly as in the app |

Every text pair the page uses is listed in `landing.source.json` → `contrast` and must reach **7:1 in dark and sun**, the app's own bar.

### Type

The app's scale ([Typography](../foundations/Typography/README.md)), with web sizes derived from it. No sentence below 16px; body is 18px, the app's `body-large`.

| Web style | Phone (< 720px) | Wide (≥ 720px) | Derived from |
|---|---|---|---|
| `.l-display` (hero title) | 40/44, 700 | 56/60, 700 (`--l-display-lg`) | `glance-numeral`, × 1.4 when wide |
| `.l-title` (section title) | 28/36, 700 | 40/44, 700 (`--l-display-md`) | `headline-medium`, then `glance-numeral` |
| `.l-h3` (card title) | 24/30, 700 | same | `glance-title` |
| `.l-lead` (intro) | 18/26 | 22/32 (`--l-lead`) | `body-large`, then `title-large` size at body weight |
| body | 18/26 | same | `body-large` |
| `.l-small` | 16/24 | same | `body-medium` |
| chip | 14/18, 700 | same | `label-medium` |
| eyebrow, nav, buttons | 16/20, 700 | same | `label-large` |

Roboto Flex is served with the page when it is bundled (as in the app, never from a font service); until then the system sans.

### Space, size and shape

| Token | Value | From |
|---|---|---|
| spacing | 4px scale | `--space-*`, unchanged |
| `--l-section` / `--l-section-phone` | 96 / 64px | `space-huge` × 2 / `space-giant` |
| `--l-max` | 1120px | content width |
| `--l-measure` | 68ch | reading width for intros |
| `--l-gutter` | 24px | `space-xxl` |
| `--l-btn-height` | 64px | `touch-min`: the page is read on phones in the street too |
| `--l-chip-height` | 32px | app chip visual height; inside a 64px+ row |
| `--l-card-radius` | 28px | `radius-xl` |
| `--l-card-pad` | 24px | `space-xxl` |

Breakpoints: 720px (two columns, display sizes) and 960px (hero split, paired cards).

## 3. Components

| Component | Built from | Rules |
|---|---|---|
| **Preview ribbon** | Snackbar colours | Above the header, only while the page is a preview. Generated; empty and hidden once the launch release is out. |
| **Release line** | — | The eyebrow above the hero title: "Echo 1.0 · Android 12 and later · Free and open source", from the latest release and the catalog's minSdk. |
| **Header** | Top app bar | Wordmark with the radar glyph, four anchors. No hamburger: anchors wrap. |
| **Hero** | — | Release line, display title, lead, CTA, facts row, map illustration. |
| **Facts row** | — | Short facts with a check. Each item carries `data-fact` naming a claim that is Available or a design claim; anything else fails the check. |
| **Button** | Button | Primary (filled `primary`) and outlined. 64px tall, `radius-lg`. Sentence case, the verb of what happens. |
| **Map illustration** | Map style, Report pin, Expiry indicator, Mesh status | App map tokens, app pin shapes with expiry rings, dotted hops between phones. Labelled "Illustration"; one `role="img"` with a description, everything inside `aria-hidden`. |
| **Step card** | Card | Number in `glance-numeral` + `primary`, title, one or two sentences. Three in a row when wide. |
| **Claim row** | List item | Icon, text, status chip. Written by the build from the register; never hand-edit inside `data-claim`. |
| **Status chip** | Chip | One colour per state: Available `safe-container`; In development and Coming in `primary-container`; Planned `surface-container-highest`; Checked by the build / By design `secondary-container`; Known limit `blocked-container`; Not planned outlined. |
| **Protects / doesn't pair** | Card | Two equal cards; the doesn't card is never smaller, lower contrast or collapsed. |
| **Pin legend** | Report pin | One tile per family, with shape named in words ("A diamond"). |
| **Audience card** | Card | Title, one sentence, one link. No status claims in prose: point to the chips. |
| **FAQ** | List item | `<details>`/`<summary>`, 64px rows, works without a script. |
| **Footer** | — | Source, vulnerability reporting, licence, sibling project, one sentence on what the page does. |

## 4. Page anatomy

1. Preview ribbon (before the launch only)
2. Header
3. Hero
4. How it works: three steps, then "What it does" claims
5. Read at arm's length: pin legend
6. What Echo protects, and what it doesn't
7. Don't take our word for it: the review, release signing, build-checked claims, threat model, private reporting
8. What's next: features after this release
9. What Echo won't do
10. Who it's for
11. Questions
12. Footer

The order follows the message layers in [MESSAGING.md](MESSAGING.md) §4. Sections alternate `surface` and `surface-container-low`.

## 5. Accessibility

Same bar as the app ([Accessibility](../foundations/Accessibility/README.md)): 7:1 text, 3:1 non-text, 44px+ targets for inline links and 64px for buttons and FAQ rows, visible focus ring in `secondary`, a skip link, one `h1`, logical headings, landmarks, `lang`. Pins never rely on colour alone; the legend names each shape. Logical properties (`inset-inline-start`) everywhere, for right-to-left later.

## 6. What the page may load and send

Checked by `tools/landing.py check`:

- No `<script>`, inline event handler, `<form>`, `<iframe>`, `<object>` or `<embed>`.
- No `src`, `href` or CSS `url()` that points to another site, except plain `<a>` links the reader chooses to follow.
- `<meta name="referrer" content="no-referrer">`.

Not checkable here, and stated on the page instead: whoever hosts the page sees the reader's IP address. Hosting is an open question (below).

## 7. The claim register

`landing.source.json` → `claims`. Each claim has an `id` (used as `data-claim` on the page) and a `kind`:

| Kind | Needs | Words from |
|---|---|---|
| `feature` | `ships_with`: work items; optional `spec` id | `text`, or a `protects` row |
| `design` | `gate` (an id in `.ai/gates.toml`) or `file` | `text`, or a `protects` row |
| `limit` | `limit`: a row of "It doesn't protect" | WhatEchoProtects, never `text` |
| `never` | `out_of_scope`: the first cell of a SPEC.md §3.2 row | `text` |

`protects` and `limit` take their words from [What Echo protects](../screens/WhatEchoProtects/README.md), the app's wording of threat model §7; the check also refuses that page if its rows stop matching §7's count. To change a protection sentence, change the threat model, then that page; the landing page follows on the next build.

The check fails when: a cited item, spec id, gate, file or row doesn't exist; a register claim isn't on the page or the page shows one that isn't registered; the page is stale against the backlog (an item moved and nobody rebuilt); a fact, or a claim in a list marked `data-expect="available"`, isn't Available yet; a banned absolute appears.

## 8. Open questions

- **Hosting.** GitHub Pages from this repo is the simplest and sees what GitHub already sees; a self-hosted static host can promise shorter log retention. Decide before the page goes live, then say it in the FAQ.
- **Roboto Flex for the web.** Bundle the same OFL woff2 the app bundles, once the app bundles it.
- **Domain and social card.** A domain is a stable identifier visitors type into a network log; consider staying on the project's GitHub address until 1.0.
