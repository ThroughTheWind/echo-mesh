---
id: WI-035
title: Tell people when the mesh around them is too thin
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
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

2026-11-19.

- The relay keeps the peer handles it heard in the last `RelayConfig.densityWindow` (5 minutes) with when, in memory only, and exposes `recentPeers`; handles are dropped past the window and when the relay stops. `MeshState.On` carries `recentPeers`, and `lowDensity(now)` is true under `MeshNode.LOW_DENSITY_PEERS` (3) once the first 30 s of looking have passed. Both values are first guesses for the field test (WI-028, FR-16.4), which `docs/field-testing.md` now asks to measure.
- The mesh chip gets an "On, few phones" state (amber, warning icon, "Few phones · 2 nearby"; TalkBack adds that the map may be missing reports), and the mesh sheet opens with an amber callout saying an empty map doesn't mean all clear (FR-16.1).
- With nobody in range after the first 30 s, the map shows an amber banner under the top bar saying so (FR-16.2); the chip already said "No phones nearby".
- Onboarding's "Before you go" adds the carry-and-deliver line (FR-16.3).
- `LowDensityTest` on loopback drives one phone from 1 to 3 peers and back to 0, through the window, both ways.
- Design aligned: MeshStatus chip table and sheet, MapView states, OnboardingFlow step 4.

Still to do on this item: AC-27 on phones.

branch-merged-ok: the code merges with PR #6 while AC-27 waits for phones.

## Review
