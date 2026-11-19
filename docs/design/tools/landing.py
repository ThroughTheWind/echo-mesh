#!/usr/bin/env python3
"""The landing page: build it from its claim register, and refuse a page that says more than the project.

  python3 tools/landing.py build    # landing.source.json -> landing/index.html (claims, stage), landing/theme.css
  python3 tools/landing.py check    # exit 1 if the page overclaims, is stale, or reaches another site

Standard library only, like tokens.py check. Every claim on the page gets its label from the backlog
(a work item's status and its epic's release), its wording from the threat model's honest-copy list
(through screens/WhatEchoProtects), and its kind from the spec, a gate or a file. Nobody types
"Planned for 0.2.0": it is computed, so it cannot drift.
"""

import html
import json
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import tokens as T  # noqa: E402  (same folder; reuses contrast() and theme_values())

DESIGN = HERE.parent
REPO = DESIGN.parent.parent
LANDING = DESIGN / "landing"
SOURCE = LANDING / "landing.source.json"
PAGE = LANDING / "index.html"
THEME_CSS = LANDING / "theme.css"
LANDING_CSS = LANDING / "landing.css"
PROTECTS_PAGE = DESIGN / "screens" / "WhatEchoProtects" / "README.md"
THREAT_MODEL = REPO / "docs" / "threat-model.md"
SPEC = REPO / "docs" / "SPEC.md"
BACKLOG = REPO / "docs" / "backlog"
GATES = REPO / ".ai" / "gates.toml"
ICONS = DESIGN / "assets" / "icons.svg"
SPRITE = ["i-check_circle", "i-warning", "i-block", "i-schedule", "i-sync", "i-lock", "i-map", "i-radar",
          "i-add_location_alt", "i-hourglass_bottom", "i-group", "i-medical_services", "i-door_open",
          "i-local_police", "i-fence", "i-foggy", "i-water_drop", "i-shield", "i-key", "i-info", "i-download"]

LABELS = {"built": "In the code", "dev": "In development", "design-gate": "Checked by the build",
          "design-file": "By design", "limit": "Known limit", "never": "Not planned"}
ICON_FOR = {"built": "i-check_circle", "dev": "i-sync", "planned": "i-schedule", "design": "i-shield",
            "limit": "i-warning", "never": "i-block"}


class Fail(Exception):
    pass


# ------------------------------------------------------------------ sources of truth


def work_items():
    """{WI-###: {status, epic, title}} from items/ and archive/, read from front matter."""
    out = {}
    for md in list((BACKLOG / "items").glob("WI-*.md")) + list((BACKLOG / "archive").glob("WI-*.md")):
        fm = md.read_text().split("---")[1]
        get = lambda k: (re.search(rf"^{k}:\s*([^\n#]*)", fm, re.M) or [None, ""])[1].strip()  # noqa: E731
        out[get("id")] = {"status": get("status"), "epic": get("epic"), "title": get("title")}
    return out


def release_of(wi, items):
    """The release an item ships in, read from its epic's title ("…: ship 0.2.0", "… Release 1.0")."""
    epic = items.get(items[wi]["epic"]) if items[wi]["epic"] else None
    m = re.search(r"(?:ship|Release) (\d+\.\d+(?:\.\d+)?)", epic["title"] if epic else "")
    if not m:
        raise Fail(f"{wi} has no epic whose title names a release, so its claim has no target")
    return m.group(1)


def honest_copy():
    """The two numbered lists of screens/WhatEchoProtects: the app's wording of threat model §7."""
    text = PROTECTS_PAGE.read_text()
    lists = {}
    for key, head in (("protects", "**It protects**"), ("limit", "**It doesn't protect**")):
        block = text.split(head, 1)[1].split("**", 1)[0] if head in text else ""
        lists[key] = re.findall(r"^\d+\. (.+)$", block.split("\n## ")[0], re.M)
    tm = THREAT_MODEL.read_text().split("## 7.", 1)[1].split("\n## ", 1)[0]
    tm_rows = {k: len(re.findall(r"^- ", part, re.M)) for k, part in
               zip(("protects", "limit"), tm.split("**It does not protect**"))}
    for k in lists:
        if len(lists[k]) != tm_rows[k]:
            raise Fail(f"WhatEchoProtects lists {len(lists[k])} '{k}' rows, threat model §7 has {tm_rows[k]}")
    return lists


def spec_ids():
    return set(re.findall(r"^- ((?:FR|NFR)-[0-9]+\.[0-9]+[a-z]?) ", SPEC.read_text(), re.M))


def out_of_scope_rows():
    block = SPEC.read_text().split("### 3.2", 1)[1].split("\n---", 1)[0]
    return {c.strip() for c in re.findall(r"^\| ([^|]+) \|", block, re.M)}


def gate_ids():
    return set(re.findall(r'^id\s*=\s*"([^"]+)"', GATES.read_text(), re.M))


def stage_now(src, items):
    """The furthest stage whose gating item is done. The page may not claim a later one, or an earlier one."""
    reached = [s for s in src["stages"] if not s["after"] or items.get(s["after"], {}).get("status") == "done"]
    return reached[-1]


# ------------------------------------------------------------------ resolve every claim


def resolve(src):
    items, copy, specs, oos, gates = work_items(), honest_copy(), spec_ids(), out_of_scope_rows(), gate_ids()
    out = {}
    for c in src["claims"]:
        cid, kind = c["id"], c["kind"]
        text = c.get("text")
        for key in ("protects", "limit"):
            if key in c:
                rows = copy[key]
                if not 1 <= c[key] <= len(rows):
                    raise Fail(f"claim {cid}: {key} row {c[key]} does not exist in WhatEchoProtects")
                if text:
                    raise Fail(f"claim {cid}: a threat-model claim takes its words from WhatEchoProtects; remove 'text'")
                text = rows[c[key] - 1]
        if not text:
            raise Fail(f"claim {cid}: no text")
        if "spec" in c and c["spec"] not in specs:
            raise Fail(f"claim {cid}: spec id {c['spec']} is not in SPEC.md")
        if kind == "feature":
            wis = c.get("ships_with") or []
            if not wis:
                raise Fail(f"claim {cid}: a feature names the work items it ships with")
            for w in wis:
                if w not in items:
                    raise Fail(f"claim {cid}: {w} is not a work item")
            pending = [w for w in wis if items[w]["status"] != "done"]
            if not pending:
                state, label = "built", LABELS["built"]
            elif all(items[w]["status"] in ("in_progress", "review") for w in pending):
                state, label = "dev", LABELS["dev"]
            else:
                later = [w for w in pending if items[w]["status"] not in ("in_progress", "review")]
                target = max((release_of(w, items) for w in later), key=lambda v: [int(x) for x in v.split(".")])
                state, label = "planned", f"Planned for {target}"
        elif kind == "design":
            if c.get("gate"):
                if c["gate"] not in gates:
                    raise Fail(f"claim {cid}: gate {c['gate']} is not in .ai/gates.toml")
                state, label = "design", LABELS["design-gate"]
            elif c.get("file"):
                if not (REPO / c["file"]).exists():
                    raise Fail(f"claim {cid}: {c['file']} does not exist")
                state, label = "design", LABELS["design-file"]
            else:
                raise Fail(f"claim {cid}: a design claim names the gate or the file that holds it")
        elif kind == "limit":
            if "limit" not in c:
                raise Fail(f"claim {cid}: a limit cites its row in the threat model's 'does not protect' list")
            state, label = "limit", LABELS["limit"]
        elif kind == "never":
            if c.get("out_of_scope") not in oos:
                raise Fail(f"claim {cid}: '{c.get('out_of_scope')}' is not a row of SPEC.md §3.2 (out of scope)")
            state, label = "never", LABELS["never"]
        else:
            raise Fail(f"claim {cid}: unknown kind {kind}")
        out[cid] = {"text": text, "state": state, "label": label}
    return out, stage_now(src, items)


# ------------------------------------------------------------------ render


def icon(name, cls="l-ic"):
    return f'<svg class="{cls}" aria-hidden="true"><use href="#{name}"/></svg>'


def render_claim(r):
    return (f'{icon(ICON_FOR[r["state"]])}<span class="l-claim__text">{html.escape(r["text"])}</span>'
            f'<span class="l-chip l-chip--{r["state"]}">{html.escape(r["label"])}</span>')


def render_cta(stage):
    return "".join(f'<a class="l-btn l-btn--{b["style"]}" href="{html.escape(b["href"])}">{html.escape(b["label"])}</a>'
                   for b in stage["cta"])


def render_sprite():
    svg = ICONS.read_text()
    syms = []
    for name in SPRITE:
        m = re.search(rf'<symbol id="{name}"[^>]*>.*?</symbol>', svg, re.S)
        if not m:
            raise Fail(f"icon {name} is not in assets/icons.svg")
        syms.append(m.group(0))
    return '<svg xmlns="http://www.w3.org/2000/svg" aria-hidden="true" style="position:absolute;width:0;height:0;overflow:hidden">' + "".join(syms) + "</svg>"


def fill(page, attr, key, inner):
    pat = re.compile(rf'(<(\w+)[^>]*\b{attr}="{re.escape(key)}"[^>]*>)(.*?)(</\2>)', re.S)
    found = pat.findall(page)
    if len(found) != 1:
        raise Fail(f'index.html has {len(found)} elements with {attr}="{key}" (want exactly 1)')
    return pat.sub(lambda m: m.group(1) + inner + m.group(4), page)


def render_page(src, page):
    claims, stage = resolve(src)
    if src["stage"] != stage["id"]:
        raise Fail(f"landing.source.json says stage '{src['stage']}', the backlog says '{stage['id']}' "
                   f"(the stage follows the item that gates it: {stage['after'] or 'none'})")
    page = fill(page, "data-stage", "banner", html.escape(stage["banner"]))
    page = fill(page, "data-stage", "cta", render_cta(stage))
    on_page = set(re.findall(r'data-claim="([^"]+)"', page))
    if on_page - set(claims):
        raise Fail(f"index.html shows claims the register doesn't have: {sorted(on_page - set(claims))}")
    if set(claims) - on_page:
        raise Fail(f"the register has claims index.html doesn't show: {sorted(set(claims) - on_page)}")
    for cid, r in claims.items():
        page = fill(page, "data-claim", cid, render_claim(r))
    page = re.sub(r"<!-- sprite:begin -->.*?<!-- sprite:end -->",
                  lambda _m: "<!-- sprite:begin -->" + render_sprite() + "<!-- sprite:end -->", page, flags=re.S)
    return page


def render_theme_css():
    tokens = T.load(T.TOKENS)
    sun = T.theme_values(tokens)["sun"]
    lines = ["/* GENERATED by tools/landing.py from tokens.json. Do not edit by hand. */",
             "/* The site has no script, so it can't offer the app's theme switch: it follows the reader's",
             "   system setting. Dark (tokens.css :root) by default; the app's sun theme when they ask for light. */",
             "@media (prefers-color-scheme: light) {", "  :root {"]
    lines += [f"    --{k}: {v};" for k, v in sun.items()]
    lines += [f"    --weight-boost: {tokens['type']['sunWeightBoost']};", "  }", "}"]
    return "\n".join(lines) + "\n"


def build():
    src = T.load(SOURCE)
    PAGE.write_text(render_page(src, PAGE.read_text()))
    THEME_CSS.write_text(render_theme_css())
    print(f"wrote {PAGE.relative_to(REPO)} and {THEME_CSS.relative_to(REPO)}")


# ------------------------------------------------------------------ check


def visible_text(page):
    body = re.sub(r"<(svg|style|script)\b.*?</\1>", " ", page, flags=re.S | re.I)
    body = re.sub(r"<!--.*?-->", " ", body, flags=re.S)
    body = re.sub(r"<[^>]+>", " ", body)
    return html.unescape(body)


def check():
    failures = []
    src = T.load(SOURCE)
    page = PAGE.read_text()
    try:
        if render_page(src, page) != page:
            failures.append("index.html is stale or hand-edited where the build writes (run tools/landing.py build)")
        claims, _ = resolve(src)
    except Fail as e:
        failures.append(str(e))
        claims = {}
    if not THEME_CSS.exists() or THEME_CSS.read_text() != render_theme_css():
        failures.append("theme.css is stale or hand-edited (run tools/landing.py build)")

    # Short facts outside the lists (hero, meta) may only repeat what is already true today.
    for fid in re.findall(r'data-fact="([^"]+)"', page):
        if fid not in claims:
            failures.append(f"index.html states fact '{fid}', which is not a claim in the register")
        elif claims[fid]["state"] not in ("built", "design"):
            failures.append(f"index.html states '{fid}' as a fact, but it is '{claims[fid]['label']}'")

    # Honest words: the page's visible text and every claim.
    words = [w.lower() for w in src["banned"]["words"]]
    texts = [("index.html", visible_text(page))] + [(f"claim {k}", v["text"]) for k, v in claims.items()]
    for where, text in texts:
        for w in words:
            if re.search(rf"(?<![\w-]){re.escape(w)}(?![\w-])", text, re.I):
                failures.append(f"{where} says '{w}', an absolute the threat model doesn't back")

    # The site behaves like the app: nothing from another site, no script, no form, no referrer.
    for pat, why in ((r"<script\b", "runs a script"), (r"<iframe\b|<object\b|<embed\b", "embeds another page"),
                     (r"<form\b", "has a form"), (r"\son\w+\s*=", "has an inline event handler")):
        if re.search(pat, page, re.I):
            failures.append(f"index.html {why}; the landing page runs no code and sends nothing")
    for tag, attr, val in re.findall(r"<(link|img|source|use|image|video|audio|track)\b[^>]*?\b(href|src|srcset)=\"([^\"]*)\"", page, re.I):
        if re.match(r"([a-z]+:)?//", val, re.I) and not val.startswith("http://www.w3.org/"):
            failures.append(f"index.html loads {val} from another site (<{tag} {attr}>)")
    if not re.search(r'<meta name="referrer" content="no-referrer">', page):
        failures.append('index.html needs <meta name="referrer" content="no-referrer">: following a link must not tell the next site where the reader came from')
    for css in (LANDING_CSS, THEME_CSS):
        text = css.read_text() if css.exists() else ""
        if re.search(r"@import|url\(\s*['\"]?([a-z]+:)?//", text, re.I):
            failures.append(f"{css.name} loads something from another site")
    # Component rules from the design system: no literal colours, no palette tones, nothing below 13px.
    lcss = LANDING_CSS.read_text()
    for m in re.finditer(r"#[0-9a-fA-F]{3,8}\b|\brgba?\(|\bhsla?\(|--ref-", lcss):
        failures.append(f"landing.css uses {m.group(0)}: components use semantic roles, never a hex value or a palette tone")
    for m in re.finditer(r"font-size:\s*(\d+)px", lcss):
        if int(m.group(1)) < 13:
            failures.append(f"landing.css sets font-size {m.group(1)}px: nothing below 13px")

    # Contrast: the page's text pairs, at the app's minimum.
    tv = T.theme_values(T.load(T.TOKENS))
    rules = src["contrast"]
    n = 0
    for tid in rules["themes"]:
        for fg, bg in rules["pairs"]:
            n += 1
            r = T.contrast(tv[tid][fg], tv[tid][bg])
            if r < rules["min"]:
                failures.append(f"{tid}: {fg} on {bg} is {r:.2f}:1 (min {rules['min']})")

    if failures:
        print("FAILED\n" + "\n".join(failures))
        sys.exit(1)
    states = {}
    for r in claims.values():
        states[r["label"]] = states.get(r["label"], 0) + 1
    print(f"ok: {len(claims)} claims resolve (" + ", ".join(f"{v} {k.lower()}" for k, v in sorted(states.items()))
          + f"), {n} contrast pairs, page fresh, no outside requests, no absolutes")


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else ""
    try:
        {"build": build, "check": check}.get(cmd, lambda: sys.exit(__doc__))()
    except Fail as e:
        print("FAILED\n" + str(e))
        sys.exit(1)
