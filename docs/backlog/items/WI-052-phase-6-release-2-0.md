---
id: WI-052
title: Phase 6 — Release 2.0: organisations, packs and a second radio
type: epic
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F28, F29, F30, F31, F32, F33, F34, AC-40, AC-46]
epic: 
children: [WI-053, WI-054, WI-055, WI-056, WI-057, WI-058, WI-059, WI-060, WI-061, WI-062, WI-063, WI-064, WI-065, WI-066, WI-067]
---

## Proposal (rationale)

Phase 6 of the [roadmap](../../ROADMAP.md): recognisable organisations (unions, associations, media) without a server, plans everyone carries, a crisis catalog, Wi-Fi Aware for bulk transfer, and sharing Echo itself offline. One combined crypto design and one outside review cover the organisation layer.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Every child item is `done`.
- 2.0.0 is published after the outside review of the organisation layer and Wi-Fi Aware identifiers.

### Impacts

- Biggest change since 1.0: a new trust layer and a second transport. Needs funding for the review (extended spec D8).

### Approach

- WI-053 (design acceptance) first, then trust store (WI-056) before roles (WI-057, WI-058) and announcements (WI-059). Catalogs (WI-060) before packs (WI-062). The Wi-Fi Aware spike (WI-063) before the adapter (WI-064) and its uses (WI-065, WI-066). Review (WI-054) before the release (WI-067).

### Acceptance criteria / tests

- All children done; AC-40 to AC-46 on the reference phones; review findings fixed or accepted in writing.

### Out of scope

- Circles, group management at scale, areas to avoid, LoRa, legacy build (epic WI-068).

## Execution

## Review
