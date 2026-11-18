---
id: WI-039
title: Phase 5 — Release 1.1: groups through the whole day
type: epic
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F20, F21, F22, F23, F24, F25, F26, F27, AC-32, AC-39]
epic: 
children: [WI-040, WI-041, WI-042, WI-043, WI-044, WI-045, WI-046, WI-047, WI-048, WI-049, WI-050, WI-051]
---

## Proposal (rationale)

Phase 5 of the [roadmap](../../ROADMAP.md): what affinity groups need before and after the march, not only during it, from the [extended spec](../../SPEC-EXTENDED.md) §9 (1.1). None of it adds a new trust model: every feature reuses the group frame or stays on the phone.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Every child item is `done`.
- 1.1.0 is published; AC-32 to AC-39 hold on the reference phones.

### Impacts

- Threat model §11 and the ADR-0004 amendment must be accepted before WI-041, WI-047 and WI-048 start (WI-040).

### Approach

- WI-040 lands first. WI-041 (phrases) comes before WI-042 (rally point) and WI-044 (check-ins), which reuse it. The rest are independent.
- WI-051 cuts the release last.

### Acceptance criteria / tests

- All children done; AC-32 to AC-39 re-run on the 1.1.0 release candidate.

### Out of scope

- Organisation keys, roles, packs and Wi-Fi Aware (epic WI-052, Phase 6).

## Execution

## Review
