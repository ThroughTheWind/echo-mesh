---
id: WI-061
title: Move phrases and announcements into the catalog
type: chore
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [FR-20.3, FR-32.1]
epic: WI-052
children: []
---

## Proposal (rationale)

Phrases (WI-041) and announcement phrases (WI-059) are defined in code until catalogs exist; FR-32.1 makes them catalog entries so organisations and languages can extend them.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Phrases and announcement phrases load from the catalog with labels per language.
- Wire ids stay stable across the move.

### Impacts

- `core/model`, catalog format (WI-055).

### Approach

- A mechanical move once WI-060 lands.

### Acceptance criteria / tests

- Existing phrase frames decode to the same meaning before and after.

### Out of scope

- New phrases; nothing deferred.

## Execution

## Review
