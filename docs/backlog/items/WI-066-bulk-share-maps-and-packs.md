---
id: WI-066
title: Share maps and event packs over Wi-Fi Aware
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [FR-2.4, F31, AC-45]
epic: WI-052
children: []
---

## Proposal (rationale)

One person with internet should be able to equip a group with the map and the pack on the day, without a share sheet dance.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Send a map file or pack to a nearby phone over Wi-Fi Aware with the same import review as a file.
- BLE-only phones unaffected.

### Impacts

- `app` (share flows), Wi-Fi Aware adapter (WI-064).

### Approach

- Reuse the import paths of WI-012 (maps) and WI-062 (packs).

### Acceptance criteria / tests

- AC-45: a 50 MB map transfers between two Wi-Fi Aware phones.

### Out of scope

- Gossiping maps or packs automatically to everyone: not planned.

## Execution

## Review
