#!/usr/bin/env python3
"""Design tokens for the street design system.

  python3 tools/tokens.py generate   # sources -> tokens.json (needs `pip install materialyoucolor`)
  python3 tools/tokens.py build      # tokens.json -> tokens.css, Color.kt, preview gallery (stdlib only)
  python3 tools/tokens.py check      # contrast + generated files are fresh; exit 1 on any failure

Run from docs/design/. `check` is meant to be a rungs gate (fast tier): it needs no third-party
package, so CI does not install the colour generator.
"""

import json
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
DESIGN = HERE.parent
SRC_COLOR = HERE / "tokens.source.json"
SRC_FOUND = HERE / "foundations.source.json"
TOKENS = DESIGN / "tokens.json"
CSS = DESIGN / "components" / "tokens.css"
GALLERY = DESIGN / "preview" / "index.html"
COMPONENTS = DESIGN / "components"
# Path inside the app repo once it exists. Package root is a placeholder until the app is named.
KOTLIN_PACKAGE = "io.github.throughthewind.echo.core.designsystem.theme"
COLOR_KT = DESIGN.parent.parent / "core" / "designsystem" / "src" / "main" / "kotlin" / "io" / "github" / "throughthewind" / "echo" / "core" / "designsystem" / "theme" / "Color.kt"

M3_ROLES = [
    ("primary", "Accent: Report FAB, your position, primary buttons, the mesh-on state."),
    ("on-primary", "Text and icons on primary."),
    ("primary-container", "Selected chips, position-sharing bar."),
    ("on-primary-container", "Text and icons on primary-container."),
    ("secondary", "Focus ring."),
    ("on-secondary", "Text on secondary."),
    ("secondary-container", "Tonal buttons, group member avatars."),
    ("on-secondary-container", "Text and icons on secondary-container."),
    ("tertiary", "Unused in the MVP; reserved."),
    ("on-tertiary", "Reserved."),
    ("tertiary-container", "Reserved."),
    ("on-tertiary-container", "Reserved."),
    ("error", "Panic wipe, destructive actions, failures."),
    ("on-error", "Text and icons on error."),
    ("error-container", "Wipe sheet header, error banners."),
    ("on-error-container", "Text on error-container."),
    ("surface", "Canvas behind sheets and screens; pin halo."),
    ("on-surface", "Primary text and icons."),
    ("on-surface-variant", "Secondary text, captions, ages."),
    ("surface-container-lowest", "Map overlay base in dark."),
    ("surface-container-low", "List backgrounds."),
    ("surface-container", "Bottom sheets."),
    ("surface-container-high", "Cards, list rows, map chips."),
    ("surface-container-highest", "Pressed rows, picker tile base."),
    ("outline", "Borders that mark a target's edge (3:1)."),
    ("outline-variant", "Dividers."),
    ("inverse-surface", "Snackbar."),
    ("inverse-on-surface", "Snackbar text."),
    ("inverse-primary", "Snackbar action."),
    ("scrim", "Scrim behind sheets."),
]


def load(p):
    return json.loads(Path(p).read_text())


def hx(argb):
    return "#%06x" % (argb & 0xFFFFFF)


def to_int(h):
    return int(h.lstrip("#"), 16)


def luminance(h):
    v = to_int(h)
    chans = [((v >> s) & 255) / 255 for s in (16, 8, 0)]
    lin = [c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in chans]
    return 0.2126 * lin[0] + 0.7152 * lin[1] + 0.0722 * lin[2]


def contrast(a, b):
    la, lb = sorted([luminance(a), luminance(b)])
    return (lb + 0.05) / (la + 0.05)


# ---------------------------------------------------------------- generate (needs materialyoucolor)


def generate():
    from materialyoucolor.dynamiccolor.material_dynamic_colors import MaterialDynamicColors as M
    from materialyoucolor.hct import Hct
    from materialyoucolor.palettes.tonal_palette import TonalPalette
    from materialyoucolor.scheme.scheme_monochrome import SchemeMonochrome
    from materialyoucolor.scheme.scheme_vibrant import SchemeVibrant

    src = load(SRC_COLOR)["color"]
    found = load(SRC_FOUND)
    seed = Hct.from_int(0xFF000000 | to_int(src["seed"]["value"]))
    schemes = {"vibrant": SchemeVibrant, "monochrome": SchemeMonochrome}

    def camel(n):
        a, *rest = n.split("-")
        return a + "".join(w.capitalize() for w in rest)

    values = {}  # token -> theme -> hex
    for th in src["themes"]:
        tid = th["id"]
        s = schemes[th["scheme"]](seed, th["isDark"], th["contrastLevel"], spec_version="2021")
        for role, _ in M3_ROLES:
            values.setdefault(role, {})[tid] = hx(getattr(M, camel(role)).get_argb(s))
        n = s.neutral_palette
        palettes = {"primary": s.primary_palette, "secondary": s.secondary_palette,
                    "neutral": n, "neutral-variant": s.neutral_variant_palette, "error": s.error_palette}
        for role, (pal, tone) in th.get("overrides", {}).items():
            values[role][tid] = hx(palettes[pal].tone(tone))
        values.setdefault("pin-halo", {})[tid] = values["surface"][tid] if th["isDark"] else "#ffffff"

        tones = src["familyTones"][tid]
        for fam, spec in ((k, v) for k, v in src["families"].items() if k != "note"):
            chroma = spec["chroma"] if tones["chromaCap"] is None else min(spec["chroma"], tones["chromaCap"])
            pal = TonalPalette.from_hue_and_chroma(spec["hue"], chroma)
            for suffix, key in [("", "base"), ("on-", "on"), ("", "container"), ("on-", "onContainer")]:
                name = {"base": fam, "on": f"on-{fam}", "container": f"{fam}-container",
                        "onContainer": f"on-{fam}-container"}[key]
                values.setdefault(name, {})[tid] = hx(pal.tone(tones[key]))

        g = src["groups"]
        for name, hue in zip(g["names"], g["hues"]):
            pal = TonalPalette.from_hue_and_chroma(hue, g["chroma"][tid])
            values.setdefault(name, {})[tid] = hx(pal.tone(g["tones"][tid]))

        mp = src["map"][tid]
        accent = seed.hue
        safe_hue = src["families"]["safe"]["hue"]
        for key, tone in mp.items():
            if key == "water":
                c = hx(TonalPalette.from_hue_and_chroma(accent, 6 if tid == "discreet" else 20).tone(tone))
            elif key == "park":
                c = hx(TonalPalette.from_hue_and_chroma(safe_hue, 4 if tid == "discreet" else 14).tone(tone))
            else:
                c = hx(n.tone(tone))
            values.setdefault(f"map-{key}", {})[tid] = c
    # Road casing exists only in sun (white roads on light land need an edge); elsewhere it equals the road.
    for tid in values["map-road-major"]:
        if tid not in values.get("map-road-casing", {}):
            values.setdefault("map-road-casing", {})[tid] = values["map-road-major"][tid]

    usage = dict(M3_ROLES)
    usage["pin-halo"] = "Ring that separates a public report pin from the map."
    for fam, spec in src["families"].items():
        if fam == "note":
            continue
        usage[fam] = f"Pin fill, {spec['shape']} shape. {spec['usage']}"
        usage[f"on-{fam}"] = f"Icon on a {fam} pin."
        usage[f"{fam}-container"] = f"Picker tile and detail header for {fam} reports."
        usage[f"on-{fam}-container"] = f"Text and icons on {fam}-container."
    for i, name in enumerate(src["groups"]["names"], 1):
        usage[name] = f"Halo and monogram badge of the user's group number {i}."
    for k in values:
        if k.startswith("map-"):
            usage[k] = "Offline map style: " + k[4:].replace("-", " ") + "."

    tokens = {
        "name": load(SRC_COLOR)["name"],
        "version": 1,
        "meta": {"source": "GENERATED by tools/tokens.py from tools/*.source.json. Do not edit by hand."},
        "color": {
            "note": f"Material 3 roles from seed {src['seed']['value']} ({src['seed']['note']}) plus fixed report "
                    "families, group halos and the offline map style. Dark is the default theme.",
            "themes": [{"id": t["id"], "name": t["name"], "note": t["note"]} for t in src["themes"]],
            "tokens": [{"name": k, "value": v, "usage": usage.get(k, "")} for k, v in values.items()]
            + [{"name": src["seed"]["name"], "value": src["seed"]["value"], "usage": "Seed of the accent."}],
            "families": {k: {"shape": v["shape"], "usage": v["usage"]} for k, v in src["families"].items() if k != "note"},
            "contrast": src["contrast"],
        },
        **found,
    }
    TOKENS.write_text(json.dumps(tokens, indent=1, ensure_ascii=False) + "\n")
    print(f"wrote {TOKENS.relative_to(DESIGN)}")


# ---------------------------------------------------------------- build (stdlib)


def theme_values(tokens):
    out = {}
    for t in tokens["color"]["tokens"]:
        if isinstance(t["value"], dict):
            for tid, v in t["value"].items():
                out.setdefault(tid, {})[t["name"]] = v
    return out


def render_css(tokens):
    tv = theme_values(tokens)
    lines = ["/* GENERATED by tools/tokens.py from tokens.json. Do not edit by hand. */"]
    sel = {"dark": ":root, [data-theme=\"dark\"]", "sun": "[data-theme=\"sun\"]", "discreet": "[data-theme=\"discreet\"]"}
    for tid, vals in tv.items():
        lines.append(sel[tid] + " {")
        lines += [f"  --{k}: {v};" for k, v in vals.items()]
        if tid == "sun":
            lines.append(f"  --weight-boost: {tokens['type']['sunWeightBoost']};")
        lines.append("}")
    root = [":root {", f"  --font-brand: {tokens['type']['families']['brand']};", "  --weight-boost: 0;"]
    for sec in ("spacing", "radius", "shadow", "size", "opacity"):
        for t in tokens[sec]["tokens"]:
            root.append(f"  --{t['name']}: {t['value']};")
    for g in tokens["type"]["groups"]:
        for s in g["styles"]:
            root.append(f"  --{s['name']}: {s['fontWeight']} {s['fontSize']}/{s['lineHeight']} var(--font-brand);")
            root.append(f"  --{s['name']}-tracking: {s['letterSpacing']};")
    root.append("}")
    return "\n".join(lines + root) + "\n"


def kt_name(n):
    a, *rest = n.split("-")
    return a + "".join(w.capitalize() for w in rest)


def render_kotlin(tokens):
    tv = theme_values(tokens)
    m3 = [r for r, _ in M3_ROLES if r not in ("scrim",)]
    out = [
        "// GENERATED by docs/design/tools/tokens.py from docs/design/tokens.json. Do not edit by hand.",
        f"package {KOTLIN_PACKAGE}",
        "",
        "import androidx.compose.material3.ColorScheme",
        "import androidx.compose.material3.darkColorScheme",
        "import androidx.compose.material3.lightColorScheme",
        "import androidx.compose.runtime.Immutable",
        "import androidx.compose.ui.graphics.Color",
        "",
    ]
    def c(v):
        return f"Color(0xFF{v.lstrip('#').upper()})"
    for tid, vals in tv.items():
        fn = "lightColorScheme" if tid == "sun" else "darkColorScheme"
        out.append(f"internal val {tid}Scheme: ColorScheme =")
        out.append(f"    {fn}(")
        for r in m3:
            out.append(f"        {kt_name(r)} = {c(vals[r])},")
        out.append(f"        background = {c(vals['surface'])},")
        out.append(f"        onBackground = {c(vals['on-surface'])},")
        out.append(f"        surfaceVariant = {c(vals['surface-container-highest'])},")
        out.append(f"        scrim = {c(vals['scrim'])},")
        out.append("    )")
        out.append("")
    street = [n for n in next(iter(tv.values())) if n not in dict(M3_ROLES)]
    out.append("/** Report families, group halos and the map style: not part of Material's ColorScheme. */")
    out.append("@Immutable")
    out.append("data class StreetColors(")
    out += [f"    val {kt_name(n)}: Color," for n in street]
    out.append(")")
    out.append("")
    for tid, vals in tv.items():
        out.append(f"internal val {tid}StreetColors =")
        out.append("    StreetColors(")
        out += [f"        {kt_name(n)} = {c(vals[n])}," for n in street]
        out.append("    )")
        out.append("")
    return "\n".join(out).rstrip() + "\n"


def render_gallery(tokens):
    cards = []
    for d in sorted(p for p in COMPONENTS.iterdir() if p.is_dir()):
        prev = d / "preview.html"
        if not prev.exists():
            continue
        html = prev.read_text()
        m = re.match(r"<!--\s*@dsCard(.*?)-->", html)
        meta = dict(re.findall(r'(\w+)="([^"]*)"', m.group(1))) if m else {}
        body = html[m.end():] if m else html
        cards.append((meta.get("group", "Other"), d.name, meta.get("subtitle", ""), body))
    order = ["Foundations", "Map", "Reports", "Mesh", "Groups", "Safety", "Onboarding", "Other"]
    cards.sort(key=lambda c: (order.index(c[0]) if c[0] in order else 99, c[1]))
    parts = [
        "<!DOCTYPE html>",
        "<!-- GENERATED by tools/tokens.py from components/*/preview.html. Do not edit by hand. -->",
        '<html lang="en" data-theme="dark"><head><meta charset="utf-8">',
        '<meta name="viewport" content="width=device-width, initial-scale=1">',
        "<title>Echo design system</title>",
        "<style>", render_css(tokens), (COMPONENTS / "bundle.css").read_text(), "</style>",
        "</head><body>",
        sprite(),
        '<header class="gallery-head"><h1>Echo design system</h1>',
        '<p>Every component in three themes. One self-contained file: nothing here loads from the network.</p>',
        '<div class="theme-switch" role="radiogroup" aria-label="Theme">',
    ]
    for t in tokens["color"]["themes"]:
        parts.append(f'<button type="button" data-set-theme="{t["id"]}">{t["name"]}</button>')
    parts.append("</div></header><main>")
    for group, name, sub, body in cards:
        parts.append(f'<section class="card" id="{name}"><div class="card-head"><span class="card-group">{group}</span>'
                     f'<h2>{name}</h2><p>{sub}</p></div><div class="card-body">{body.strip()}</div></section>')
    parts.append("</main>")
    parts.append("<script>document.querySelectorAll('[data-set-theme]').forEach(b=>b.addEventListener('click',()=>"
                 "{document.documentElement.dataset.theme=b.dataset.setTheme;}));"
                 "if(location.hash.startsWith('#theme='))document.documentElement.dataset.theme=location.hash.slice(7);</script>")
    parts.append("</body></html>")
    return "\n".join(parts) + "\n"


def sprite():
    """Only the <symbol>s of icons.svg, so metadata a file store adds to the SVG doesn't leak into the gallery."""
    symbols = re.findall(r"<symbol .*?</symbol>", (COMPONENTS / "icons.svg").read_text(), re.S)
    return '<svg xmlns="http://www.w3.org/2000/svg" style="display:none">\n' + "\n".join(symbols) + "\n</svg>"


def outputs():
    tokens = load(TOKENS)
    return {CSS: render_css(tokens), COLOR_KT: render_kotlin(tokens), GALLERY: render_gallery(tokens)}


def build():
    for path, text in outputs().items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
        print(f"wrote {path}")


# ---------------------------------------------------------------- check (stdlib)


def check():
    tokens = load(TOKENS)
    tv = theme_values(tokens)
    rules = tokens["color"]["contrast"]
    failures = []
    report = []
    for tid, vals in tv.items():
        for kind, pairs, minimum in (("text", rules["text"], rules["textMin"][tid]),
                                     ("non-text", rules["nonText"], rules["nonTextMin"])):
            for fg, bg in pairs:
                r = contrast(vals[fg], vals[bg])
                report.append(f"{tid:9} {kind:8} {fg} on {bg}: {r:.2f} (min {minimum})")
                if r < minimum:
                    failures.append(report[-1])
    for path, text in outputs().items():
        if not path.exists() or path.read_text() != text:
            failures.append(f"stale or hand-edited: {path} (run tools/tokens.py build)")
    if "-v" in sys.argv:
        print("\n".join(report))
    if failures:
        print("FAILED\n" + "\n".join(failures))
        sys.exit(1)
    print(f"ok: {len(report)} contrast pairs across {len(tv)} themes, generated files fresh")


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else ""
    {"generate": generate, "build": build, "check": check}.get(cmd, lambda: sys.exit(__doc__))()
