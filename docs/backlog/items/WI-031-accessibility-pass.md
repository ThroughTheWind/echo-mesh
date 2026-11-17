---
id: WI-031
title: Accessibility pass
type: chore
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [NFR-11, NFR-12]
epic: WI-029
children: []
---

## Proposal (rationale)

People with screen readers, larger text or limited dexterity are at protests too.

Phase 4 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Content descriptions everywhere; font scaling; WCAG AA contrast (the design tokens gate covers colours).
- TalkBack can drop and read a report end to end.
- Primary touch targets ≥ 56 dp checked on every screen.

### Impacts

- `app`, `core/designsystem`.

### Approach

- Compose UI tests with semantics assertions where practical.

### Acceptance criteria / tests

- TalkBack walkthrough of drop and read report recorded; automated semantics tests pass in CI.

### Out of scope

- Translations: not planned (NFR-17).

## Execution

## Review
