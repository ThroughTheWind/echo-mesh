---
id: WI-012
title: Show an offline map: renderer wrapper, import, browser download and sharing
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [FR-2.1, FR-2.2, FR-2.3, FR-2.4, FR-2.5, FR-2.6, AC-2, AC-3]
epic: WI-006
children: []
---

## Proposal (rationale)

Reports are only useful on a map, and the map must work with the network cut and without the app ever holding `INTERNET`.

Phase 1 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- `:core:map` wraps the renderer chosen in WI-004 behind a small API (show region, place pins, own position).
- Import a map file through the Android file picker; download via the browser then import (FR-2.3).
- Share a map file phone to phone through the share sheet (FR-2.4).
- Own position from the platform `LocationManager`, never stored (FR-2.5). Manage maps: list, size, delete (FR-2.6).

### Impacts

- `core/map`, `app`. The manifest gate and `checkForbiddenDependencies` must stay green with the renderer added.

### Approach

- Follow the ADR from WI-004; any telemetry switch in the renderer is off and covered by a test or gate.

### Acceptance criteria / tests

- On the OPPO Find X3 Pro, the oldest reference phone, the map draws smoothly with the measurements WI-004 took on the Pixel 11 Pro (first render, frames dropped while panning, memory).
- AC-2: a region downloaded or imported at home renders with flight mode on.
- AC-3: a map file shared from one phone and imported on another renders identically.
- `checkManifestPermissions` passes on debug and release.

### Out of scope

- Pins and the report UI: WI-013. Maps in the panic wipe: WI-024.

## Execution

## Review
