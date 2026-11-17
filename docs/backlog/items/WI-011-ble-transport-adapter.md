---
id: WI-011
title: Build the BLE transport adapter and the relay foreground service
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
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

## Review
