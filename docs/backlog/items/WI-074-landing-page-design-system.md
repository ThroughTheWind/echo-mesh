---
id: WI-074
title: Design the landing page system and its messaging, derived from the app's tokens
type: docs
status: review
branch: claude/project-thread-tga93b
created: 2026-11-19
updated: 2026-11-19
related: [WI-026, WI-030, WI-038, NFR-14, ADR-0003]
epic:
children: []
---

## Proposal (rationale)

Echo needs a public page that tells protesters, organisers and human-rights groups what it is and why it's different, quickly. It is also where the project is most tempted to overclaim: Echo is pre-alpha, and a page that reads like a finished product could put someone on the street with a tool that isn't ready. The page needs a design system grounded in the app's, and a messaging direction whose honesty is checked, not just intended.

## Decision

`accepted` — 2026-11-19, Antoine, by asking for the landing page design system, grounded in the app's, with a study of how to communicate Echo's intent and value.

## Plan

### Requirements

- A landing design system that adds no colour or font to the app's: semantic roles from `tokens.css`, the app's type scale, the app's report pins and map style.
- A positioning and messaging direction: audiences, message layers, words, stage-driven CTAs, objections.
- Every feature or protection line on the page carries a status computed from the backlog, and protection lines use the app's wording of threat model §7.
- The page loads nothing from another site and runs no script.
- A reference page built only from the system.

### Impacts

- New `docs/design/landing/`, new `docs/design/tools/landing.py`, a new fast-tier gate `landing-claims` in `.ai/gates.toml`, a link from `docs/design/README.md`.
- No app code, no spec or threat-model change.

### Approach

- Claims live in a register (`landing.source.json`); `tools/landing.py build` writes their chips, the stage banner and CTA into `index.html`, and `check` fails on a stale page, an unknown citation, a stage the backlog hasn't reached, a fact that isn't true yet, a banned absolute, an outside request, or a text pair under 7:1.
- Options considered: hand-typed status labels (rejected: they drift, and drift here is overclaiming); generating the whole page (rejected: the prose and layout are design work, only the facts need to be machine-owned).

### Acceptance criteria / tests

- `python3 -B docs/design/tools/landing.py check` passes, and fails on a planted banned word, an outside image, a wrong stage and a fact stated before it's built.
- `python3 -B docs/design/tools/tokens.py check` and `node .ai/rungs.mjs check` pass.
- The reference page renders in dark and light with no network request.

### Out of scope

- Hosting, domain and deployment of the page (open question in `landing/README.md`); French and Spanish copy (WI-037); bundling Roboto Flex for the web. Nothing deferred to a new item yet.

## Execution

Branch `claude/project-thread-tga93b` (the session's assigned branch, in place of `feature/WI-074-…`).

## Review

- `landing.py check`: passes. Planted failures, each reported: stage set to `1.0` (backlog says `pre-alpha`), `data-fact="wipe"` (Planned for 0.3.0), "anonymous" in the hero, an `<img>` from another site.
- Rendered with Playwright at 1280px (dark and light) and 390px (dark): no request left `file:`.
