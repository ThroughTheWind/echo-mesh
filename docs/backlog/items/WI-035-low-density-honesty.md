---
id: WI-035
title: Tell people when the mesh around them is too thin
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [F16, US-23, FR-16.1, FR-16.2, FR-16.3, FR-16.4, AC-27]
epic: WI-029
children: []
---

## Proposal (rationale)

An empty map in a thin crowd reads as "all clear". The app must say when it is likely missing reports.

Phase 4 of the [roadmap](../../ROADMAP.md), from the [extended spec](../../SPEC-EXTENDED.md)'s 1.0 additions.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the extended spec's 1.0 additions ([SPEC-EXTENDED.md](../../SPEC-EXTENDED.md)).

## Plan

### Requirements

- A low-density state in the mesh status when fewer than 3 distinct peer tokens were seen in 5 minutes, as a tunable constant (FR-16.1).
- With zero peers, the map says so plainly (FR-16.2).
- Onboarding explains carry-and-deliver: walking with the mesh on helps reports cross gaps (FR-16.3).

### Impacts

- `core/mesh` (counting distinct session tokens, never storing them), `app` (mesh status, map, onboarding copy).

### Approach

- Count tokens in memory over a sliding window; the threshold is tuned by the field test (WI-028, FR-16.4).

### Acceptance criteria / tests

- AC-27: with fewer than the threshold of peers the mesh status shows the low-density state; with none, the map says so.
- A loopback test drives the peer count across the threshold in both directions.

### Out of scope

- Adaptive scanning (F26, 1.1).

## Execution

## Review
