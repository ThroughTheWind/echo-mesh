---
id: WI-007
title: Prove BLE discovery and exchange on the three reference phones
type: spike
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [SPEC-A1, NFR-9, NFR-10, NFR-13, FR-3.3]
epic: WI-006
children: []
---

## Proposal (rationale)

Every BLE item rests on behaviour only read about: background scanning under Doze, the foreground service, extended advertising, and whether ColorOS kills the relay. Settling it on the real phones first avoids building the adapter on a wrong assumption.

Phase 1 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- A throwaway debug app (or debug screen) advertises the service UUID, scans, connects over GATT or L2CAP, and moves one ~150-byte message each way.
- Run on each pair of the Pixel 11 Pro, Pixel 9a and OPPO Find X3 Pro: screen on, screen off for 15 minutes, app in background with the `connectedDevice` foreground service.
- Record per phone: Android version and patch, extended advertising, LE 2M and Coded PHY, L2CAP CoC support, time to discover, and whether the service survived the OEM battery manager.
- Use only the permissions planned in NFR-10 (`BLUETOOTH_SCAN` with `neverForLocation`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`); no location.

### Impacts

- Findings update `docs/research/` and spec A1; the device table feeds WI-011 and `docs/transports.md`.

### Approach

- Antoine runs the debug APK from CI; results go in this item's Execution section. Spike code stays on its `spike/` branch and does not merge.

### Acceptance criteria / tests

- A results table covering every phone and every condition above.
- A decision recorded here: GATT or L2CAP for the data path, and whether extended advertising is used.
- Spec A1 has its final status.

### Out of scope

- The production adapter: WI-011. Battery measurement over hours: WI-028 (field test).

## Execution

## Review
