---
id: WI-013
title: Build the Phase 1 screens: onboarding draft, map with pins, drop a report, mesh status
type: feature
status: review
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [F1, F3, F4, F5, FR-4.7, FR-5.6, FR-7.2, NFR-11, AC-1, AC-4]
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
- A 5-second send delay with a visible Cancel after confirming (FR-4.7).
- The "Reports near you" list, with the same styles and detail as the map (FR-5.6); the group filter comes with WI-022.
- Mesh on/off control and status (FR-3.1, FR-3.2, FR-3.5).
- Every screen built from `docs/design/` components with `StreetTheme`.

### Impacts

- `app`, `core/designsystem`. Strings externalized (NFR-17).

### Approach

- One screen per PR is fine; this item closes when all five exist.

### Acceptance criteria / tests

- AC-1: first launch shows the protects / does-not-protect screens before any permission request.
- AC-4 end to end through the UI on two reference phones.
- Primary touch targets ≥ 56 dp (NFR-11), checked against the design specs.

### Out of scope

- Final onboarding copy: WI-026. Group audience and filters: WI-022.

## Execution

- **Mesh node** (`core/mesh/node/MeshNode`): the screens' one door to the mesh. Off at first (FR-3.1), on and off from the chip and the sheet, the 5-second grace period with Cancel before anything is signed (FR-4.7), withdrawal of the user's own reports (FR-4.5), and the reports to show with how many hops they travelled (FR-5.5). The relay now counts phones in range and notes when another phone's report last arrived (FR-3.2), counts only. Tested on the loopback simulator (`MeshNodeTest`, and a status test in `RelaySimulationTest`).
- **Design system** (`core/designsystem/component`): `ReportPin` (family shape and colour, solid for a group and hollow with a dashed edge for anyone nearby, the expiry ring, badges, yours, probably gone), the expiry ring, bar and labels, `MeshStatusChip` with every state of the spec, and a dashed border. The icon sprite's 49 glyphs are generated as vector drawables by `tools/tokens.py build`, and its check fails on a stale or orphaned one.
- **Screens** (`app/…/ui`): onboarding in four steps with no permission asked (FR-1.1, FR-1.2, AC-1); What Echo protects; the map with pins over the VTM map through `MapCamera`, the mesh chip, centre on me (location asked in context with our sheet first), the Report button, the picker with its crosshair and the sending bar; the report detail sheet with the honest provenance box and Withdraw; the mesh sheet with the approximate facts, radios by capability and the honesty callout; Reports near you, sorted by distance from the user or the map centre (FR-5.6). Strings in `strings.xml`.
- **Honest by construction:** onboarding and What Echo protects leave out the panic wipe and discreet mode until they ship (WI-024, WI-025), so the app never claims a protection it does not have yet.
- **Not here:** clustering below zoom 16 and the street render theme come with the map style work; votes with WI-019; groups and the filter with WI-022; the Nearby devices and Notifications sheets with the BLE adapter (WI-011), which is the first radio that needs them.
- **Still to do, with the phones:** AC-4 end to end through the UI on two reference phones, and checking the touch targets on a device (NFR-11).

## Review

Merged to `main` in PR #5 on 2026-11-19, at Antoine's request ("Accept and merge the prs"). AC-4 passed through the UI on two Pixels. Open: the touch-target check on a device (NFR-11).
