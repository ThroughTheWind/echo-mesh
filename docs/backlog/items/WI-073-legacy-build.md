---
id: WI-073
title: Offer a legacy build for Android 8 to 11
type: feature
status: deferred
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F39, R5]
epic: WI-068
children: []
---

## Proposal (rationale)

minSdk 31 excludes older phones; if many users are excluded, a separate build that asks for location with an explicit warning may be worth its honesty cost.

## Decision

`deferred` — 2026-11-17, Antoine, by accepting the [extended spec](../../SPEC-EXTENDED.md), which starts this only when its evidence condition is met. Revisit when: field diagnostics or user reports show a significant share of intended users on Android 8–11.

## Plan

### Requirements

- A separate build flavour for Android 8–11 with the location prompt explained.

### Impacts

- Build logic, permissions, copy, test matrix.

### Approach

- Plan in full when the trigger is met.

### Acceptance criteria / tests

- Written when it starts.

### Out of scope

- Everything until the trigger is met.

## Execution

## Review
