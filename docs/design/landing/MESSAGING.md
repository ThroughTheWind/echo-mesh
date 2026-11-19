**Authoritative for:** what the landing page says, to whom, in what order, and what it may never say.
**Not authoritative for:** what Echo protects (the [threat model](../../threat-model.md) §7), what it does (the [spec](../../SPEC.md)), or how the page looks ([README.md](README.md)).

# Landing page: positioning and messaging

The landing page has one job: in five seconds, a stranger understands what Echo is and why it's different; in five minutes, a sceptic finds every limit and every source. It must do both without saying one word more than the project can back today.

## 1. The situation the page is written for

- **The page goes live with Echo 1.0, and not before** (Antoine, 2026-11-19). By then Echo has had its independent security review (WI-030), groups, wipe and discreet mode are in, and the release key is published (WI-038). So the page speaks as a released product: "Get Echo" is the main action and the chips say "Available". Until then it exists as a preview, under a ribbon that says so, and `tools/landing.py check --deploy` refuses to publish it (section 6).
- **The page must still never run ahead of the release.** Nobody types "Available": a chip says it only when the release the feature ships in is out. Anything after 1.0 sits in "What's next", labelled with its release, never in "What it does". The check fails if it moves.
- **Marchers and trainers arrive together.** At launch, marchers can act on the page directly, and digital-security trainers and human-rights organisations decide whether to recommend Echo. Marchers often adopt what their trainers recommend. So the page leads with the marcher's value (that is what makes anyone get it in five seconds) and then serves the sceptic, in depth.
- **Its readers are suspicious, and should be.** This audience has seen apps advertised as "secure" and "anonymous" get broken in public. Every absolute word costs trust. A clear list of what Echo *doesn't* protect earns more of it than any promise.
- **Visiting the page can itself be a risk signal.** The page is built like the app: no script, no trackers, nothing loaded from another site, no referrer sent (README §6).

## 2. Audiences, in order of who acts first

| Audience | What they need to see | What they do next | Where on the page |
|---|---|---|---|
| **People marching** (the product's primary persona) | What it is, that it works without signal, how to get ready | Get Echo and the map at home | Hero, how it works, legend, FAQ |
| **Trainers and human-rights groups** (gatekeepers) | The limits, the threat model, the review, the signing key, the build gates | Read the threat model; recommend Echo or tell us what's missing | Protects / doesn't, "Don't take our word for it", FAQ |
| **Organisers, affinity groups, medics** | Groups joined in person, positions, being findable then gone, and what's next for groups | Join in person and try a report together before the day | How it works, What's next, Who it's for |
| **Developers, reviewers, translators** | Stack, openness, how to start | Contributing guide, SECURITY.md | Who it's for, footer |
| **Journalists and the curious** | A one-line explanation and what it doesn't do | Quote the one-liner and the limits | Hero, protects / doesn't |

## 3. Positioning

**For** people at a protest **who** need to know what's happening around them when the phone network is down or overloaded, **Echo is** a shared street map that nearby phones keep up to date by passing short-lived reports to each other over Bluetooth. **Unlike** chat apps, it carries reports that expire, not conversations; **unlike** apps that need a server, there is no server, no account and no internet involved.

**The category is "a map", not "a messenger".** Everything in Echo's market that people have heard of is a messenger (Signal, Briar, and the offline messengers that were broken in public). Calling Echo a messenger puts it in that comparison and invites the question "is it as good as Signal?". Calling it a map of the street puts it alone, and makes the honest answer to "should I use Signal instead?" easy: use both, for different things.

**The differentiator is structural, not a promise.** "No server, no account, no internet" is true by construction and checked by a build gate (ADR-0003). It explains *why* Echo works when the network doesn't, and it's the one safety-adjacent fact the page can say in the hero today without a label.

## 4. The message, layer by layer

| Layer | Reader's time | Says | Page element |
|---|---|---|---|
| 1. What | 5 s | See what's happening around you. No internet needed. | Hero title |
| 2. How | 15 s | Phones nearby pass short-lived reports over Bluetooth, on an offline map, and they expire. | Hero lead, facts row, map illustration |
| 3. What release | 5 s | Echo 1.0, the Android versions it runs on, free and open source. | Release line above the title (computed) |
| 4. What it does today, and next | 1 min | Each feature with its status: available, or planned for 1.1 or 2.0. | Claim chips (computed), What's next |
| 5. Can I trust it | 5 min | What it protects and what it doesn't, word for word from the app and the threat model; what the build refuses; what Echo won't do. | Protects / doesn't, Check it, Won't do |
| 6. Next step | — | Get Echo, next to Read what it protects. | CTA |

### Hero title: recommended and alternatives

| Option | Why | Risk |
|---|---|---|
| **See what's happening around you. No internet needed.** (recommended) | Leads with the benefit a marcher feels; the second sentence is the differentiator, and is true today. | "No internet needed" must not drift into "can't be tracked": the doesn't-protect list sits on the same page. |
| A map of the street, passed from phone to phone. | Most distinctive; names the category and the mechanism. | Slower to understand for someone who doesn't know what a mesh is. Good for the meta description and social cards. |
| When the network goes down, the street still talks. | Emotional, quotable. | Implies Echo works at any density, which it doesn't: a thin crowd is a thin mesh (F16). |

Never use "Waze for protests" on the page. It is a useful shorthand in the README for developers, but it names a Google product and suggests live routing and a server, both of which Echo refuses.

## 5. Words

The app's [content and voice](../foundations/ContentAndVoice/README.md) rules apply unchanged: plain, honest, calm; sentence case; no exclamation marks. The page adds:

| Use | Never | Why |
|---|---|---|
| "can be read only by people who joined the group in person" | private, encrypted (as a selling word), secure | Say who can read it, not an adjective. The precise sentence is the threat model's. |
| "nothing you send carries your name, number or a fixed ID" | anonymous, untraceable, invisible | The first line of the doesn't-protect list says Echo is detectable. |
| "planned for 0.2.0", "in development" | coming soon, now with | Dates and features slip; a computed label doesn't. |
| "how to get ready for a protest" | battle-tested, protest-proven | Echo has a review behind it, not a war record. |
| "nearby phones confirmed" | verified | A public count proves nothing (FR-6.5). |
| "help and exits" | safe, safe zone, safe route | No place is safe, and Echo won't suggest routes (spec §3.2). |
| "don't take our word for it" | trusted by, military-grade, audited (before WI-030 is done) | Earned claims only. |

`tools/landing.py check` refuses the banned words in the page's visible text (`landing.source.json` → `banned`). The list is deliberately strict; a word comes off it only with a reason in this file.

## 6. Launch, preview and deploy

`landing.source.json` → `launch` names the release the page goes live with (1.0) and the item that cuts it (WI-033). The check reads the backlog:

| Moment | When | Chips are computed as if | Ribbon | `check --deploy` |
|---|---|---|---|---|
| Preview | WI-033 isn't done | every item of a release up to 1.0 were done | "Preview: this is the page Echo will publish with 1.0…" | Fails |
| Live | WI-033 is done | nothing: the real backlog | None | Passes |

The preview is what the page will say on launch day, so it can be reviewed now. Its ribbon is generated and can't be removed by hand, and whoever adds the deploy workflow makes it run `python3 -B docs/design/tools/landing.py check --deploy` first. After the launch, the release line follows the latest release in `releases` (1.1, 2.0) and later features move from "Planned" to "Available" on their own.

The CTA is the same in both: "Get Echo" (GitHub releases) and, next to it with equal size, "Read what it protects".

## 7. Claim labels

Every feature or protection line on the page carries a chip. The chip is computed by `tools/landing.py` from the work items the claim `ships_with`; nobody types it.

| Chip | Means | Computed from |
|---|---|---|
| Available | Every item it ships with is `done`, and the release they ship in is out | Item status, epic title, `releases` |
| Coming in 1.1 | Done on `main`, not released yet | Item status, epic title |
| In development | What's left is `in_progress` or `review` | Item status |
| Planned for 1.1 | What's left is later; the version is the release named by the item's epic | Item status, epic title |
| Checked by the build | A gate fails the build if it stops being true | `.ai/gates.toml` |
| By design | Fixed by a file in the repo (the licence) | The file |
| Known limit | A line of the threat model's "does not protect" | WhatEchoProtects |
| Not planned | A row of the spec's out-of-scope table | SPEC.md §3.2 |

Lists of what Echo does carry `data-expect="available"`: a claim in them that isn't Available or a design claim fails the check, so a feature can't slip into the present tense. Protection lines take their words from [What Echo protects](../screens/WhatEchoProtects/README.md), the app's own wording of threat model §7, so the page and the app can't drift apart.

## 8. Objections the page must answer

| Objection | Answer, in the page's words | Source |
|---|---|---|
| How do I get ready? | At home: install, get the map, read both lists, join your group in person and try a report. | Spec F2, F8 |
| Can the police tell I'm using it? | Yes, with a receiver nearby. | TM §7, doesn't protect 1 |
| Does it hide who I am? | Designed so that nothing you send carries your name, number or a fixed ID; that's not the same as impossible to find. | TM §7, protects 3 and doesn't 5 |
| Can a report be fake? | Yes. Treat a public report as a tip, not a fact. | TM §7, doesn't 2 |
| Why not Signal or Briar? | Use them for messages; Echo is a shared map of short-lived reports. | Spec §3.2 |
| Is this the real app? | Only from this project's releases, each saying who signed it and how to check. | WI-038 |
| Does this page track me? | No script, no cookies, nothing from other sites; the host sees your IP. | README §6 |

## 9. Imagery

- **No photos of protests or protesters.** Faces endanger people (the same reason Echo carries no photos), and riot imagery adds alarm the voice rule forbids.
- **The app's own pins on the app's own map style.** The first time someone opens Echo, the pins are already familiar. Shapes and colours come from the [Report pin](../components/ReportPin/README.md) and the map tokens, never redrawn.
- **Illustrations are labelled "Illustration".** A mocked "9 nearby phones" is not a measurement.

## 10. Translations

English first, like the app; French and Spanish follow WI-037 (F18). Layout and copy follow the app's rules for 20–30% longer text. The claims register and the banned list are translated with the page, and the banned list grows per language ("sécurisé", "anónimo").
