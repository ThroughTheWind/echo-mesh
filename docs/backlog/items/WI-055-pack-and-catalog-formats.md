---
id: WI-055
title: Decide the event pack and catalog formats
type: docs
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F31, F32, FR-31.6, NFR-23]
epic: WI-052
children: []
---

## Proposal (rationale)

Packs and catalogs are new signed inputs from outside the phone; like the wire format, they need a fixed, versioned, fuzzable format decided before code.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- ADR(s) for the catalog format (FR-32.1) and the pack format (FR-31.1), each versioned with a size cap (256 KB for packs).
- Decoders fuzzed in CI (NFR-23).

### Impacts

- `docs/decisions/`, `core/model`, fuzz targets.

### Approach

- Follow ADR-0005's approach: fixed layout where possible, size check before parse, signature before processing.

### Acceptance criteria / tests

- ADRs accepted by Antoine; fuzz targets in CI with no crash.

### Out of scope

- Pack contents UI (WI-062), catalog UI (WI-060).

## Execution

## Review
