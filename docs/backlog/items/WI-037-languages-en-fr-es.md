---
id: WI-037
title: Ship Echo in English, French and Spanish
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [F18, US-25, FR-18.1, FR-18.2, FR-18.3, FR-18.4, FR-18.5, NFR-21, AC-29]
epic: WI-029
children: []
---

## Proposal (rationale)

People at a protest must understand exactly what the app protects, in their language. Antoine decided English, French and Spanish at 1.0, extensible later.

Phase 4 of the [roadmap](../../ROADMAP.md), from the [extended spec](../../SPEC-EXTENDED.md)'s 1.0 additions.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the extended spec's 1.0 additions ([SPEC-EXTENDED.md](../../SPEC-EXTENDED.md)).

## Plan

### Requirements

- English, French and Spanish complete; the system language is the default, with an in-app override (FR-18.1).
- Adding a language is a strings file plus a glossary entry, documented in `docs/translating.md`; no code change (FR-18.2).
- The safety set (onboarding, "What this app protects", wipe, QR warning, steering warnings) is reviewed by a fluent speaker against threat model §7 before a language ships (FR-18.3).
- Layouts use start and end, never left and right (FR-18.4). Report labels always ship with their icon (FR-18.5).
- A CI gate fails on any missing string in a shipped language, and a pseudo-locale run catches truncation and hard-coded text (NFR-21).

### Impacts

- `app` and `core/designsystem` resources, CI, `docs/translating.md`, AGENTS.md (the English-only convention ends).

### Approach

- Android string resources per locale; the gate compares keys across locales.

### Acceptance criteria / tests

- AC-29: the app runs fully in French and Spanish with no missing or truncated strings, and the safety set has a recorded fluent review.
- The missing-string gate fails on a planted missing French string.

### Out of scope

- Right-to-left languages (prepared for, not shipped).

## Execution

2026-11-19.

- French and Spanish drafts of every string, in `values-fr/` and `values-es/` beside both English files (app and design system), in the English order with the English comments kept. Dates already come from the system's formatter, so they follow the language.
- `scripts/gates/locales.mjs` (fast tier, with a self-test that includes a planted missing French string): every shipped language has every string, nothing extra, the same format arguments, and plurals with `one` and `other`; and no layout says left or right (FR-18.4). It found none.
- `res/xml/locales_config.xml` and `android:localeConfig`: Android 13 and later offer Echo's own language setting. Debug builds enable the `en-XA` and `ar-XB` pseudo-locales for the truncation run.
- `docs/translating.md`: where strings live, how to add a language, the review rule for the safety set, a glossary (maillage / malla for mesh, nasse / encapsulamiento for kettle, and so on), and a review record. AGENTS.md's English-only line now points there.
- Deviation: no in-app language override yet, because there is no settings screen to hold it; F-005 records the missing screen, and the override is its language row. Android 13+ users have the system's per-app setting meanwhile; Android 12 users follow the phone's language.

Still to do on this item, all needing people or phones: the fluent review of the safety set in French and in Spanish (FR-18.3), recorded in `docs/translating.md`; the pseudo-locale and French and Spanish runs on a phone (AC-29); the in-app override with the settings screen.

branch-merged-ok: the drafts and the gate merge with PR #6 while the reviews and AC-29 wait for people and phones.

## Review
