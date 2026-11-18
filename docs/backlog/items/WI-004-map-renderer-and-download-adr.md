---
id: WI-004
title: Choose the offline map renderer and how map data arrives
type: spike
status: review
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
- **Phone test, Pixel 11 Pro (Android 17, 120 Hz), 2026-11-18**, on a throwaway local branch (`spike/WI-004-vtm`, not pushed), with `ile-de-france.map` from the Mapsforge download server (210,258,216 bytes):
  - No `INTERNET` permission: `checkManifestPermissions` green, and `aapt2` and `dumpsys package` list only AndroidX's `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, present without VTM too. No location permission. `checkForbiddenDependencies` green.
  - APK content +1.64 MB in a debug build (code 1.07 MB before R8, native 46 KB arm64 and 44 KB armv7, theme assets about 480 KB).
  - First full render 1.16–1.28 s after the screen opens over 3 cold starts; the file opens in 0.26–0.32 s.
  - 30 s of scripted pan, zoom (levels 9 to 17) and rotate: the map layer drew 3,081 frames with 0 dropped (SurfaceFlinger timestats; gfxinfo sees nothing on a GL surface). Smooth to the eye, labels stay upright while rotating.
  - Memory after a minute: 264–271 MB total PSS, about 155 MB of it graphics.
  - No crash, ANR or VTM warning in logcat.
  - The Mapsforge Canvas fallback on the same map and script: last tiles at 1.5–2.3 s, grey blank tiles after zooming, rotation that jumps, labels upside down, 395 MB total PSS. VTM is the better choice; the fallback stays a fallback.
  - `vtm-themes` and `mapsforge-themes` ship the same asset paths, so both in one APK needs a `pickFirsts` packaging rule.
- Deviation: the OPPO Find X3 Pro, the oldest reference phone, was not measured. The same check runs there when WI-012 builds the real `:core:map` wrapper, before 0.1.0.

## Review
