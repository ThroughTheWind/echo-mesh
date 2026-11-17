---
id: WI-013
title: Build the Phase 1 screens: onboarding draft, map with pins, drop a report, mesh status
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [F1, F3, F4, F5, FR-7.2, NFR-11, AC-1, AC-4]
epic: WI-006
children: []
---

## Proposal (rationale)

The first screens a person sees. They must be honest about what the app protects and usable one-handed in the street, following the design system.

Phase 1 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Onboarding first draft (FR-1.1, FR-1.2): what the app protects and what it does not, before any permission; permissions one at a time, each with its reason. Copy traced to threat model §7.
- Map screen with report pins (type, age, time left, direct or relayed; FR-5.5) and expiry indicator (FR-7.2).
- Drop a report in two taps when the pin is placed (FR-4.2); public audience only in this phase.
- Mesh on/off control and status (FR-3.1, FR-3.2, FR-3.5).
- Every screen built from `docs/design/` components with `StreetTheme`.

### Impacts

- `app`, `core/designsystem`. Strings externalized (NFR-17).

### Approach

- One screen per PR is fine; this item closes when all four exist.

### Acceptance criteria / tests

- AC-1: first launch shows the protects / does-not-protect screens before any permission request.
- AC-4 end to end through the UI on two reference phones.
- Primary touch targets ≥ 56 dp (NFR-11), checked against the design specs.

### Out of scope

- Final onboarding copy: WI-026. Group audience and filters: WI-022.

## Execution

## Review
