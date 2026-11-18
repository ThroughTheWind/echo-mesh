---
id: WI-062
title: Import and show event packs
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F31, US-40, FR-31.1, FR-31.2, FR-31.3, FR-31.4, FR-31.5, AC-43]
epic: WI-052
children: []
---

## Proposal (rationale)

A plan everyone carries works with zero peers: route, meeting and dispersal points, stations, rights text.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Pack contents per FR-31.1, signed by a trusted issuer (FR-31.2); group-signed packs only after threat model §12.3's re-wrap design is reviewed.
- Shared by file, Wi-Fi Aware or small over BLE; never fetched by the app (FR-31.3).
- Signer shown at import; organisation keys inside need separate acceptance (FR-31.4).
- Map layer, expiry, removal, wiped (FR-31.5).

### Impacts

- `core/model` (pack decoder per WI-055), `core/map` (layer), `app` (import, review), jail-support card rights text (WI-045).

### Approach

- Organisation-signed packs first; group-signed packs as a second step inside this item once §12.3 is reviewed, otherwise split into a new item.

### Acceptance criteria / tests

- AC-43: imported from a file, layer shows with zero peers, keys need separate acceptance, gone at expiry.

### Out of scope

- Sharing packs over Wi-Fi Aware (WI-066).

## Execution

## Review
