---
id: WI-011
title: Build the BLE transport adapter and the relay foreground service
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [FR-3.1, FR-3.3, FR-3.3a, FR-3.4, FR-3.5, FR-14.2, NFR-4, NFR-10, AC-4, AC-5, AC-6a, AC-19]
epic: WI-006
children: []
---

## Proposal (rationale)

The first real radio. Everything above it is tested on loopback; this is where the mesh meets the street.

Phase 1 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- `:adapters:ble` implements `MeshTransport` and passes `MeshTransportContract`, as the spike (WI-007) decided (GATT or L2CAP data path; extended advertising as a capability).
- Rotating app tokens in lock-step with the OS address, at most every 10 minutes; the device name is never advertised (FR-3.4) and a test or gate proves it.
- A `connectedDevice` foreground service started only when the user turns the mesh on with the app visible; stopping the mesh stops all radio activity (FR-3.3).
- Permission and radio-state reporting for the mesh status (FR-3.5); detection of an unexpected stop (FR-3.3a).
- Registered in `TransportRegistration.kt` for release builds.

### Impacts

- `adapters/ble`, `app` (service, registration, manifest permissions of NFR-10). The manifest gate stays green: BLE needs no `INTERNET`.

### Approach

- Thin adapter: frames in, frames out. No parsing or relaying in the adapter (`.ai/rules/transports.md`).

### Acceptance criteria / tests

- AC-4: a public report from phone A appears on phone B within 10 s, on every pair of reference phones.
- AC-5: B keeps receiving with the screen off and the app in the background.
- AC-6a: with A and C out of range and B between them, a report from A reaches C through B.
- Contract tests pass for the adapter (instrumented or with a fake BLE stack).

### Out of scope

- The 30-minute capture proving rotation (AC-19): WI-027. Wi-Fi Aware: after 1.0.

## Execution

2026-11-19, first pass, on the Phase 1 branch (PR 5).

- **Adapter** (`adapters/ble`): `BleTransport` holds the logic on a `BleRadio` port; `AndroidBleRadio`
  is the Android half. Legacy advertising (service UUID in the advertisement, a 6-byte token and PSM
  as service data in the scan response), filtered scanning, insecure L2CAP channels with 2-byte
  length framing, as WI-007 decided. Of two phones the lower token connects and says which
  advertisement it answers in a 4-byte hello. Token, PSM and advertising restart together every
  8 to 10 minutes (jittered) and every link drops, so phones that met reconnect as strangers. A
  `PeerHandle` is 8 random bytes per link. Limits: 8 links, 12 new connections a minute, 5 s for
  the hello. A session that cannot start (Bluetooth turned off under it) is retried every 5 s, and
  a radio error ends one loop, never the app.
- **Tests on the JVM** against `FakeBleRadio`: `MeshTransportContract`, rotation (new token, PSM and
  handle, no token or PSM reused), a hostile length and a silent link (each drops one link, not the
  phone), the start refusal with its reason, the retry, framing, advertisement decoding, and
  `AdvertisementPrivacyTest`, which reads `AndroidBleRadio.kt` and fails if any advertisement builder
  may include the device name or anything that names the phone, or if a GATT server or pairing
  appears.
- **Radio state** (FR-3.5): `MeshTransport.start()` throws `TransportUnavailableException` with
  `RADIO_OFF`, `PERMISSION_MISSING` or `UNSUPPORTED`; `MeshNode` shows it as
  `MeshState.Unavailable`, and the chip reads "Bluetooth off · Fix" (Android's turn-on dialog),
  "Permission needed · Fix" (our sheet once, then Echo's page in Settings) or "Can't run here".
- **Relay service** (FR-3.3): `MeshService`, `connectedDevice`, not sticky, started by `App.meshOn()`
  only after the mesh turned on from a tap, stopped by `App.meshOff()` or its notification's Turn
  off. Low importance, silent, secret on the lock screen, updated at most every 30 s.
- **Permissions in context** (FR-1.2, NFR-10): our Nearby devices sheet, then our Notifications
  sheet, each before Android's dialog and only the first time; a refusal still turns the mesh on so
  the chip carries the fix.
- **Unexpected stop** (FR-3.3a): `Settings.meshMeantOn` is set on turn-on and cleared on turn-off.
  Found set with the mesh off at launch, the chip reads "Relaying stopped · Fix" and the sheet offers
  Android's battery-optimisation settings. The mesh never turns itself back on. Per-manufacturer
  steps are not built yet.
- **Registration**: release registers BLE; debug registers BLE too, so development phones reach each
  other, and falls back to the simulated radio on a phone or emulator without Bluetooth LE.

Not yet proven: AC-4, AC-5 and AC-6a on the reference phones (next, over the test bench). Whether
restarting a legacy advertisement changes the OS address with the token is not something the app
controls; the 30-minute capture of AC-19 (WI-027) measures it.

## Review
