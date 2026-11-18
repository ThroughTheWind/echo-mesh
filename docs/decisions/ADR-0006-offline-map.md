---
id: ADR-0006
title: Draw the map with VTM from Mapsforge files that the user downloads in a browser and imports
status: accepted
date: 2026-11-17
supersedes:
superseded_by:
---

# ADR-0006 — Draw the map with VTM from Mapsforge files that the user downloads in a browser and imports

> Accepted by Antoine on 2026-11-18.

## Context

The map is half the product (spec FR-2), and it has to work where the network is cut. The app holds
no `INTERNET` permission ([ADR-0003](ADR-0003-gpl-no-servers-no-proprietary-dependencies.md)), and
any request to a tile server would tell an observer that a phone runs the app (threat model A6).
Spec assumptions A5 (an open renderer that makes no network calls) and A6 (map data without
`INTERNET`) are open, and FR-2.3 prefers a browser download followed by an import.

Forces:

- The renderer must be open source, GPL-3.0-compatible, and work with no network code path in use.
- Map data must come as one file per region that a person can fetch before the protest, keep, and
  pass to a friend, with no account and no server of ours.
- The app is small and must run on the oldest reference phone (OPPO Find X3 Pro) next to a BLE
  relay service, for hours.
- Pins and the user's own position are drawn over the map, in the street design system's colours,
  including the discreet theme.

What was checked from the published artifacts on Maven Central (2026-11-17):

| | VTM 0.25.0 | Mapsforge map 0.25.0 | MapLibre Native Android 2026.11.2 |
| --- | --- | --- | --- |
| Licence | LGPL-3.0 | LGPL-3.0 | BSD-2-Clause |
| Rendering | OpenGL ES vector | Android Canvas, from vector data | Vulkan or OpenGL vector |
| Runtime dependencies | `vtm`, AndroidSVG | map reader, AndroidSVG | OkHttp, Timber, AndroidX Fragment and others |
| Network | Optional `vtm-http` module, not used | None in the artifact | OkHttp file source built in |
| Library manifest | No permission | No permission | `INTERNET`, network state, Wi-Fi state, fine and coarse location |
| Native code | About 22 KB per ABI (tessellation) | None | 10–14 MB per ABI |
| Offline format | Mapsforge `.map` | Mapsforge `.map` | MBTiles or PMTiles, plus a style, glyphs and sprites |
| Last release | April 2025 | April 2025 | November 2026 |

## Decision

- **Renderer: VTM** (`org.mapsforge:vtm-android`), wrapped by `:core:map`, which is the only module
  that imports it. **Fallback: Mapsforge map** (`mapsforge-map-android`), which reads the same files,
  if the spike shows VTM's OpenGL view misbehaving on a reference phone.
- **Format: Mapsforge `.map`**, one file per region, rendered with a render theme of our own built
  from the design system's tokens, including the discreet theme.
- **How map data arrives: the browser, then an import.** The app shows where to download a region
  (the Mapsforge download server or OpenAndroMaps), the user downloads it in their own browser, and
  imports it through the system file picker (`ACTION_OPEN_DOCUMENT`), which needs no permission. A
  file can also be passed between phones with any file-sharing tool and imported the same way. The
  app copies it to its private storage.
- **No in-app download.** The app never asks for `INTERNET` for maps; ADR-0003's planned exception is
  not taken.
- **Network modules stay out:** `org.mapsforge:vtm-http` and `com.squareup.okhttp3` are added to
  `gradle/forbidden-dependencies.txt`.

## Consequences

**Good**

- No network code on the classpath and no permission from the library, so the manifest gate stays
  green without overriding a library's manifest.
- A tiny footprint: about 22 KB of native code per ABI instead of 10–14 MB.
- One file per region, which a person can download at home, keep, and pass on, and which the app
  reads without a server.
- The theme is ours, so pins and the discreet theme follow the design system.

**Costs and risks**

- **Maintenance.** VTM and Mapsforge last released in April 2025. They are mature and used by apps
  on F-Droid, but if they stop working on a new Android version, fixing them is ours or the revisit
  trigger applies.
- **Two steps for the user.** Downloading in a browser and then importing is slower than a button in
  the app, and onboarding has to explain it (FR-2.3).
- **The download itself is visible.** Fetching a region from a public server before the protest is
  an ordinary web download, but it is a network event linked to a place (threat model A6). Onboarding
  says to download at home or on a different network, and that a friend's file works as well.
- **Map files are large** (tens to hundreds of megabytes per region), so they cannot travel over the
  mesh.
- **Not yet measured on a phone.** The licence, dependency and manifest facts above come from the
  published artifacts; speed, memory and drawing with no `INTERNET` permission still need the spike
  on the reference phones.

## Alternatives considered

**MapLibre Native.** The most capable and most actively maintained renderer, under a GPL-compatible
licence. Lost because its library manifest asks for `INTERNET` and location, it ships an HTTP client,
it adds 10–14 MB of native code per ABI, and an offline map needs a bundled style, fonts and sprites
next to the tiles. It could be made to work by removing permissions in our manifest and never
pointing it at a URL, but that is a network stack waiting for a mistake.

**Mapsforge map (Canvas) as the first choice.** Same files and licence, no native code at all. Kept
as the fallback: VTM draws with the GPU and handles zooming and rotating a vector map more smoothly,
which matters with one hand in a crowd.

**Raster tiles (for example osmdroid with an MBTiles archive).** Simple to draw. Lost because offline
raster regions are much larger than vector files for the same area and zoom levels, and the look
cannot follow the discreet theme.

**An in-app download confined to one module** (the fallback in FR-2.3). Simplest for the user. Lost
because it puts `INTERNET` in the manifest of an app whose promise is that it talks to nothing but
other phones, and an observer on the network would see the app itself fetch maps.

## Revisit triggers

1. The spike shows VTM and the Mapsforge Canvas view both failing on a reference phone.
2. VTM or Mapsforge no longer builds or runs on a supported Android version.
3. Field tests show people do not manage the browser-and-import flow.
4. MapLibre ships a build without network code or permissions.

## Admission check

Against [the rule](README.md): (1) constrains future work ✅ the map module, the import flow and
the theme depend on it · (2) a real alternative was rejected with a stated reason ✅ · (3) reversing
later costs meaningfully more ✅ users' saved files and our theme are tied to the format · (4) not
owned by another document ✅ the spec says what the map must do, this record says with what · (5)
not an implementation detail ✅.
