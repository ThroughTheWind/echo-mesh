---
id: WI-004
title: Choose the offline map renderer and how map data arrives
type: spike
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [FR-2.1, FR-2.3, SPEC-A5, SPEC-A6, ADR-0003]
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

- The `:core:map` wrapper and the import and share flows: WI-013.

## Execution

## Review
