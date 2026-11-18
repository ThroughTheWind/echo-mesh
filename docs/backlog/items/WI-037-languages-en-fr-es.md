---
id: WI-037
title: Ship Echo in English, French and Spanish
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
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

## Review
