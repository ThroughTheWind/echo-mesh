---
id: WI-004
title: Choose the offline map renderer and how map data arrives
type: spike
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-17
related: [FR-2.1, FR-2.3, SPEC-A5, SPEC-A6, ADR-0003, ADR-0006]
epic: WI-001
children: []
---

## Proposal (rationale)

The map is half the product, and spec assumptions A5 (an open renderer that makes no network calls) and A6 (map data without `INTERNET`) are still open. ADR-0003 says the download path needs its own ADR before anything can ask for `INTERNET`.

Phase 0 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Compare the candidate open renderers (for example MapLibre Native without telemetry, Mapsforge/VTM) on: license compatibility with GPL-3.0, network calls made by default, offline file format, APK size, rendering speed on the oldest reference phone, and maintenance.
- Prove the choice makes no network call: a debug build renders a map file with no `INTERNET` permission and the manifest gate green.
- One ADR records the renderer, the map file format, and the download path (preferred: browser download, then import; FR-2.3).

### Impacts

- New ADR; `gradle/forbidden-dependencies.txt` if a renderer pulls in telemetry; spec A5 and A6 updated.

### Approach

- A throwaway spike branch for the measurements; only the ADR and any gate change merge.

### Acceptance criteria / tests

- The ADR is accepted by Antoine and names the renderer, format and download path.
- Evidence in Execution: the renderer drew a sample region on a reference phone with no `INTERNET` permission.
- Spec A5 and A6 are no longer `Open`.

### Out of scope

- The `:core:map` wrapper and the import and share flows: WI-012.

## Execution

- Desk comparison from the published artifacts on Maven Central (licence, dependencies, library manifest, native size, last release) of VTM, Mapsforge map and MapLibre Native Android: the table is in [ADR-0006](../../decisions/ADR-0006-offline-map.md) (proposed). MapLibre's library manifest asks for `INTERNET` and location and it ships OkHttp; VTM and Mapsforge ask for nothing and carry no network module.
- ADR-0006 proposes VTM, Mapsforge `.map` files, a browser download and a system file picker import, and no in-app download. Mapsforge map (Canvas) is the fallback renderer.
- `gradle/forbidden-dependencies.txt` refuses `org.mapsforge:vtm-http` and `com.squareup.okhttp3`, with a self-test.
- Spec A5 and A6 move from Open to Proposed.
- **Still to do, on the phones:** build a throwaway debug app with VTM and no `INTERNET` permission, draw a sample region on each reference phone, and note frame rate and memory on the OPPO Find X3 Pro. Not possible here: the sandbox has no Android SDK and no phone. Antoine accepted ADR-0006 on 2026-11-18; the phone test confirms or reopens it.

## Review
