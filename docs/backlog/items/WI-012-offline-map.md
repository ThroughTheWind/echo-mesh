---
id: WI-012
title: Show an offline map: renderer wrapper, import, browser download and sharing
type: feature
status: review
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
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

- **Dependencies:** `vtm-android` and `vtm-themes` 0.25.0 in the catalog, with VTM's native tessellation library for arm64-v8a, armeabi-v7a and x86_64 (the emulator). No `vtm-http`, no OkHttp; the dependency and manifest gates run in CI.
- **`MapFileHeader`** reads the fixed start of a Mapsforge file (versions 3 to 5): magic, version, declared size, date, bounding box, and refuses anything else before the renderer sees it.
- **`MapLibrary`** keeps imported maps in the app's private storage (`files/maps`): an import is copied to a partial file, checked (a map, a supported version, a sane box, and exactly the size its header declares, so a cut-short download is refused), then renamed into place, so a refused import never replaces a good map. `list`, `delete` and `deleteAll` (for the panic wipe, FR-2.6). File names are made safe from the picker's display name.
- **`MapSharing`**: the download pages (Mapsforge and OpenAndroMaps) opened in the user's browser, the system file picker (`ACTION_OPEN_DOCUMENT`, no permission), and the share sheet through a `FileProvider` that exposes `files/maps` only (FR-2.4).
- **`OfflineMap`**, a Compose wrapper around VTM's `MapView`: one map file, buildings and labels, VTM's default theme for now, centred on the map's box, paused and resumed with the lifecycle, with a `LocationLayer` for the user's own position.
- **`OwnPosition`**: `LocationManager` updates (fused provider, GPS if there is none) as a flow, empty without permission; never stored. The map module declares coarse and fine location (FR-2.5); asking for them is WI-013's.
- **Tests:** `MapLibraryTest` (import, refusals that leave nothing behind, a refused import does not replace a good map, delete, safe names), run locally on the JVM; the Android parts compile in CI.
- **Still to do here, with the phones:** AC-2 (renders in flight mode), AC-3 (a shared file renders the same on another phone), and the OPPO Find X3 Pro measurement. The street render theme built from the design tokens (ADR-0006) comes with the map screen in WI-013, where pins and the discreet theme need it.

## Review

Merged to `main` in PR #5 on 2026-11-19, at Antoine's request ("Accept and merge the prs"). Open, with the phones: AC-2, AC-3 and the OPPO Find X3 Pro measurement.
