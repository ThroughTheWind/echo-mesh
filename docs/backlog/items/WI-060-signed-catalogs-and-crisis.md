---
id: WI-060
title: Ship signed catalogs, including Crisis, and crisis groups
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F32, US-41, FR-32.1, FR-32.2, FR-32.3, FR-32.4, FR-32.5, FR-32.6, AC-44]
epic: WI-052
children: []
---

## Proposal (rationale)

Report types must fit local reality, and Echo is protest-first, crisis-compatible (Antoine, 2026-11-17): crisis use comes only through a catalog, never by loosening a protest default.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Bundled Protest (extended) and Crisis catalogs; organisation-signed catalogs via packs (FR-32.2).
- Hard lifetime caps: 3 h protest, 12 h crisis, with 12 h retention only in groups created as crisis groups (FR-32.3).
- No free text, names, photos or history (FR-32.4); unknown types still relayed (FR-32.5).
- Onboarding offers the crisis catalog honestly (FR-32.6).

### Impacts

- `core/model` (catalog loader per WI-055), wire (catalog id), group creation (crisis flag), `app`, translations.

### Approach

- The MVP type list becomes the start of the Protest catalog; existing frames map to it unchanged.

### Acceptance criteria / tests

- AC-44: a crisis report shows correctly with the catalog and as "unknown type" (still relayed) without.

### Out of scope

- Organisation-signed catalogs arrive inside packs (WI-062).

## Execution

## Review
