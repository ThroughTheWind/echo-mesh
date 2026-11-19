**Authoritative for:** what the landing page says, to whom, in what order, and what it may never say.
**Not authoritative for:** what Echo protects (the [threat model](../../threat-model.md) §7), what it does (the [spec](../../SPEC.md)), or how the page looks ([README.md](README.md)).

# Landing page: positioning and messaging

The landing page has one job: in five seconds, a stranger understands what Echo is and why it's different; in five minutes, a sceptic finds every limit and every source. It must do both without saying one word more than the project can back today.

## 1. The situation the page is written for

- **Echo is pre-alpha.** There is no release, groups, wipe and discreet mode are planned, and the independent security review hasn't happened. A page that reads like a finished product could put someone on the street with a tool that isn't ready. That is the worst outcome the page can cause, so the stage is the first thing on it, and it is computed (section 6).
- **The readers who matter most right now aren't marchers.** Until 1.0, the people who will act on the page are digital-security trainers, human-rights organisations, organisers and contributors. Marchers adopt the tools their trainers and organisers recommend. So the page leads with the marcher's value (that is what makes anyone get it in five seconds) and then serves the sceptic, in depth.
- **Its readers are suspicious, and should be.** This audience has seen apps advertised as "secure" and "anonymous" get broken in public. Every absolute word costs trust. A clear list of what Echo *doesn't* protect earns more of it than any promise.
- **Visiting the page can itself be a risk signal.** The page is built like the app: no script, no trackers, nothing loaded from another site, no referrer sent (README §6).

## 2. Audiences, in order of who acts first

| Audience | What they need to see | What they do next | Where on the page |
|---|---|---|---|
| **Trainers and human-rights groups** (gatekeepers) | The limits, the threat model, the build gates, the review status, who's behind it | Read the threat model; follow the project; tell us what their people need | Protects / doesn't, "Don't take our word for it", FAQ |
| **Organisers, affinity groups, medics** | Groups, positions, being findable then gone, and when each lands | Watch the status chips; test with their group | How it works, status chips, Who it's for |
| **People marching** (the product's primary persona) | What it is, that it works without signal, that it isn't ready yet | Come back when the banner changes | Hero, banner, legend |
| **Developers, reviewers, translators** | Stack, openness, how to start | Contributing guide, SECURITY.md | Who it's for, footer |
| **Journalists and the curious** | A one-line explanation and an honest status | Quote the one-liner and the banner | Hero, banner |

## 3. Positioning

**For** people at a protest **who** need to know what's happening around them when the phone network is down or overloaded, **Echo is** a shared street map that nearby phones keep up to date by passing short-lived reports to each other over Bluetooth. **Unlike** chat apps, it carries reports that expire, not conversations; **unlike** apps that need a server, there is no server, no account and no internet involved.

**The category is "a map", not "a messenger".** Everything in Echo's market that people have heard of is a messenger (Signal, Briar, and the offline messengers that were broken in public). Calling Echo a messenger puts it in that comparison and invites the question "is it as good as Signal?". Calling it a map of the street puts it alone, and makes the honest answer to "should I use Signal instead?" easy: use both, for different things.

**The differentiator is structural, not a promise.** "No server, no account, no internet" is true by construction and checked by a build gate (ADR-0003). It explains *why* Echo works when the network doesn't, and it's the one safety-adjacent fact the page can say in the hero today without a label.

## 4. The message, layer by layer

| Layer | Reader's time | Says | Page element |
|---|---|---|---|
| 1. What | 5 s | See what's happening around you. No internet needed. | Hero title |
| 2. How | 15 s | Phones nearby pass short-lived reports over Bluetooth, on an offline map, and they expire. | Hero lead, facts row, map illustration |
| 3. Is it ready | 5 s, but first | Pre-alpha: not ready to rely on at a protest. | Stage banner (computed) |
| 4. How far along | 1 min | Each feature with its status: in the code, in development, planned for 0.x. | Claim chips (computed) |
| 5. Can I trust it | 5 min | What it protects and what it doesn't, word for word from the app and the threat model; what the build refuses; what Echo won't do. | Protects / doesn't, Check it, Won't do |
| 6. Next step | — | Depends on the stage (section 6). | CTA (computed) |

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
| "isn't ready to rely on at a protest" | beta, early access | Says the consequence, not the jargon. |
| "nearby phones confirmed" | verified | A public count proves nothing (FR-6.5). |
| "help and exits" | safe, safe zone, safe route | No place is safe, and Echo won't suggest routes (spec §3.2). |
| "don't take our word for it" | trusted by, military-grade, audited (before WI-030 is done) | Earned claims only. |

`tools/landing.py check` refuses the banned words in the page's visible text (`landing.source.json` → `banned`). The list is deliberately strict; a word comes off it only with a reason in this file.

## 6. Stage, CTA and banner

The page has three stages. The current one is **computed from the backlog**, not chosen: a stage is reached only when the item that gates it is `done`, and the check fails if `landing.source.json` names any other stage.

| Stage | Reached when | Banner | Primary CTA | Secondary |
|---|---|---|---|---|
| `pre-alpha` | Always | Pre-alpha. There is no release yet, and Echo isn't ready to rely on at a protest. | Read what it protects | Follow the code |
| `test-builds` | WI-015 (cut 0.1.0) is done | Test builds only. Echo hasn't had its independent security review, so don't rely on it at a protest yet. | Read what it protects | Get a test build |
| `1.0` | WI-033 (cut 1.0.0, after the WI-030 review) is done | Echo 1.0 has had an independent security review. Read what it protects before you rely on it. | Get Echo | Read what it protects |

Before 1.0, the page never says "Download", "Install" or "Get Echo" as its primary action. Even at 1.0, "Read what it protects" stays next to the download.

## 7. Claim labels

Every feature or protection line on the page carries a chip. The chip is computed by `tools/landing.py` from the work items the claim `ships_with`; nobody types it.

| Chip | Means | Computed from |
|---|---|---|
| In the code | Every item it ships with is `done` on `main` | Item status |
| In development | What's left is `in_progress` or `review` | Item status |
| Planned for 0.2.0 | What's left is later; the version is the release named by the item's epic | Item status, epic title |
| Checked by the build | A gate fails the build if it stops being true | `.ai/gates.toml` |
| By design | Fixed by a file in the repo (the licence) | The file |
| Known limit | A line of the threat model's "does not protect" | WhatEchoProtects |
| Not planned | A row of the spec's out-of-scope table | SPEC.md §3.2 |

"In the code" is not "released": the banner says what's released. Protection lines take their words from [What Echo protects](../screens/WhatEchoProtects/README.md), the app's own wording of threat model §7, so the page and the app can't drift apart.

## 8. Objections the page must answer

| Objection | Answer, in the page's words | Source |
|---|---|---|
| Can I use it now? | Not until the banner says it has had its security review. | Stage |
| Can the police tell I'm using it? | Yes, with a receiver nearby. | TM §7, doesn't protect 1 |
| Does it hide who I am? | Designed so that nothing you send carries your name, number or a fixed ID; that's not the same as impossible to find. | TM §7, protects 3 and doesn't 5 |
| Can a report be fake? | Yes. Treat a public report as a tip, not a fact. | TM §7, doesn't 2 |
| Why not Signal or Briar? | Use them for messages; Echo is a shared map of short-lived reports. | Spec §3.2 |
| Is this the real app? | Only from this project's releases (later F-Droid); signing is published before 1.0. | WI-038 |
| Does this page track me? | No script, no cookies, nothing from other sites; the host sees your IP. | README §6 |

## 9. Imagery

- **No photos of protests or protesters.** Faces endanger people (the same reason Echo carries no photos), and riot imagery adds alarm the voice rule forbids.
- **The app's own pins on the app's own map style.** The first time someone opens Echo, the pins are already familiar. Shapes and colours come from the [Report pin](../components/ReportPin/README.md) and the map tokens, never redrawn.
- **Illustrations are labelled "Illustration".** A mocked "9 nearby phones" is not a measurement.

## 10. Translations

English first, like the app; French and Spanish follow WI-037 (F18). Layout and copy follow the app's rules for 20–30% longer text. The claims register and the banned list are translated with the page, and the banned list grows per language ("sécurisé", "anónimo").
