#!/usr/bin/env python3
"""The landing page: build it from its claim register, and refuse a page that says more than the project.

  python3 tools/landing.py build            # landing.source.json -> landing/index.html, landing/theme.css
  python3 tools/landing.py check            # exit 1 if the page overclaims, is stale, or reaches another site
  python3 tools/landing.py check --deploy   # also exit 1 unless the launch release is out: run before publishing

Standard library only, like tokens.py check. The page is published with Echo's launch release (1.0)
and not before. Until then it is a preview: built as if every item of the launch release were done,
under a ribbon that says so, and `check --deploy` refuses it. From the launch on, every label is
computed from the real backlog. Either way nobody types "Planned for 1.1": each chip comes from a
work item's status and its epic's release, protection lines come word for word from the threat
model's honest copy (through screens/WhatEchoProtects), and other kinds cite the spec, a gate or a file.
"""

import html
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
CATALOG = REPO / "gradle" / "libs.versions.toml"
ICONS = DESIGN / "assets" / "icons.svg"
SPRITE = ["i-check_circle", "i-warning", "i-block", "i-schedule", "i-sync", "i-lock", "i-map", "i-radar",
          "i-add_location_alt", "i-hourglass_bottom", "i-group", "i-medical_services", "i-door_open",
          "i-local_police", "i-fence", "i-foggy", "i-water_drop", "i-shield", "i-key", "i-info", "i-download"]
# Android release for each API level the catalog's minSdk may name.
ANDROID = {"26": "8", "27": "8.1", "28": "9", "29": "10", "30": "11", "31": "12", "32": "12L", "33": "13",
           "34": "14", "35": "15", "36": "16", "37": "17"}

LABELS = {"available": "Available", "dev": "In development", "design-gate": "Checked by the build",
          "design-file": "By design", "limit": "Known limit", "never": "Not planned"}
ICON_FOR = {"available": "i-check_circle", "dev": "i-sync", "planned": "i-schedule", "design": "i-shield",
            "limit": "i-warning", "never": "i-block"}


class Fail(Exception):
    pass


def vkey(v):
    return [int(x) for x in v.split(".")]


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
    """The release an item ships in, read from its epic's title ("…: ship 0.2.0", "… Release 1.1")."""
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


def min_android():
    m = re.search(r'^minSdk\s*=\s*"(\d+)"', CATALOG.read_text(), re.M)
    if not m or m.group(1) not in ANDROID:
        raise Fail("gradle/libs.versions.toml has no minSdk this tool can name (update ANDROID in landing.py)")
    return ANDROID[m.group(1)]


# ------------------------------------------------------------------ the page's moment: preview or live


def moment(src, items):
    """Live once the launch release's cut item is done; until then a preview of the launch.

    In a preview, every item of a release up to the launch counts as done: the page shows the day it
    is published, and the ribbon and `check --deploy` keep it from being mistaken for today."""
    launch = src["launch"]
    for r in src["releases"]:
        if r["item"] not in items:
            raise Fail(f"release {r['version']}: {r['item']} is not a work item")
    if launch["item"] not in items:
        raise Fail(f"launch: {launch['item']} is not a work item")
    live = items[launch["item"]]["status"] == "done"
    out = {wi: dict(it) for wi, it in items.items()}
    if not live:
        for wi, it in out.items():
            if it["epic"] and it["status"] != "done":
                try:
                    if vkey(release_of(wi, items)) <= vkey(launch["version"]):
                        it["status"] = "done"
                except Fail:
                    pass
    if live:
        released = max((r["version"] for r in src["releases"] if out[r["item"]]["status"] == "done"), key=vkey)
    else:
        released = launch["version"]
    return {"live": live, "items": out, "released": released}


# ------------------------------------------------------------------ resolve every claim


def resolve(src):
    real = work_items()
    m = moment(src, real)
    items, released = m["items"], m["released"]
    copy, specs, oos, gates = honest_copy(), spec_ids(), out_of_scope_rows(), gate_ids()
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
            target = max((release_of(w, items) for w in wis), key=vkey)
            pending = [w for w in wis if items[w]["status"] != "done"]
            if not pending and released and vkey(target) <= vkey(released):
                state, label = "available", LABELS["available"]
            elif not pending:
                state, label = "dev", f"Coming in {target}"
            elif all(items[w]["status"] in ("in_progress", "review") for w in pending):
                state, label = "dev", LABELS["dev"]
            else:
                later = max((release_of(w, items) for w in pending), key=vkey)
                state, label = "planned", f"Planned for {later}"
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
    return out, m


# ------------------------------------------------------------------ render


def icon(name, cls="l-ic"):
    return f'<svg class="{cls}" aria-hidden="true"><use href="#{name}"/></svg>'


def render_claim(r):
    return (f'{icon(ICON_FOR[r["state"]])}<span class="l-claim__text">{html.escape(r["text"])}</span>'
            f'<span class="l-chip l-chip--{r["state"]}">{html.escape(r["label"])}</span>')


def render_cta(src):
    return "".join(f'<a class="l-btn l-btn--{b["style"]}" href="{html.escape(b["href"])}">{html.escape(b["label"])}</a>'
                   for b in src["cta"])


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
    claims, m = resolve(src)
    preview = "" if m["live"] else html.escape(src["preview_note"].format(launch=src["launch"]["version"]))
    page = fill(page, "data-landing", "preview", preview)
    page = fill(page, "data-landing", "release",
                html.escape(f"Echo {m['released']} · Android {min_android()} and later · Free and open source"))
    page = fill(page, "data-landing", "cta", render_cta(src))
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
    deploy = "--deploy" in sys.argv
    failures = []
    src = T.load(SOURCE)
    page = PAGE.read_text()
    m = {"live": False}
    try:
        if render_page(src, page) != page:
            failures.append("index.html is stale or hand-edited where the build writes (run tools/landing.py build)")
        claims, m = resolve(src)
    except Fail as e:
        failures.append(str(e))
        claims = {}
    if not THEME_CSS.exists() or THEME_CSS.read_text() != render_theme_css():
        failures.append("theme.css is stale or hand-edited (run tools/landing.py build)")
    if deploy and not m["live"]:
        failures.append(f"not deployable: the page launches with Echo {src['launch']['version']}, and "
                        f"{src['launch']['item']} (its release) is not done. Until then it is a preview.")

    # Short facts outside the lists (hero) and lists headed as available may only hold what is.
    for fid in re.findall(r'data-fact="([^"]+)"', page):
        if fid not in claims:
            failures.append(f"index.html states fact '{fid}', which is not a claim in the register")
        elif claims[fid]["state"] not in ("available", "design"):
            failures.append(f"index.html states '{fid}' as a fact, but it is '{claims[fid]['label']}'")
    for block in re.findall(r'<(?:ul|ol)\b[^>]*data-expect="available"[^>]*>(.*?)</(?:ul|ol)>', page, re.S):
        for cid in re.findall(r'data-claim="([^"]+)"', block):
            if cid in claims and claims[cid]["state"] not in ("available", "design"):
                failures.append(f"'{cid}' sits in a list of what Echo does, but it is '{claims[cid]['label']}': "
                                "move it to what's next, or fix its work items")

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
    for mm in re.finditer(r"#[0-9a-fA-F]{3,8}\b|\brgba?\(|\bhsla?\(|--ref-", lcss):
        failures.append(f"landing.css uses {mm.group(0)}: components use semantic roles, never a hex value or a palette tone")
    for mm in re.finditer(r"font-size:\s*(\d+)px", lcss):
        if int(mm.group(1)) < 13:
            failures.append(f"landing.css sets font-size {mm.group(1)}px: nothing below 13px")

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
    mode = "live" if m["live"] else f"preview of {src['launch']['version']}, not deployable yet"
    print(f"ok ({mode}): {len(claims)} claims resolve (" + ", ".join(f"{v} {k.lower()}" for k, v in sorted(states.items()))
          + f"), {n} contrast pairs, page fresh, no outside requests, no absolutes")


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else ""
    try:
        {"build": build, "check": check}.get(cmd, lambda: sys.exit(__doc__))()
    except Fail as e:
        print("FAILED\n" + str(e))
        sys.exit(1)
