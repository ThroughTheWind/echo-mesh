---
id: WI-007
title: Prove BLE discovery and exchange on the three reference phones
type: spike
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
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

First pair run on 2026-11-19, 00:43 to 01:17, Pixel 9a and Pixel 11 Pro on USB. The spike app (`spikes/ble-probe/`, on the local branch `spike/WI-007-ble-probe` on Antoine's PC, not pushed) advertises a fixed service UUID with a session id and an L2CAP PSM in its service data, scans with a filter on it, and moves 150 bytes each way every 60 s over GATT and over an insecure L2CAP CoC, from a `connectedDevice` foreground service. Permissions: `BLUETOOTH_SCAN` (`neverForLocation`), `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, notifications. No location.

The OnePlus Nord CE 3 Lite (Android 14) that was plugged in first was locked and left out.

### Phones

| | Pixel 9a | Pixel 11 Pro |
| --- | --- | --- |
| Android, patch | 17 (SDK 37), 2026-10-05 | 17 (SDK 37), 2026-09-01 |
| Extended / periodic advertising | yes / yes | yes / yes |
| LE 2M / Coded PHY | yes / yes | yes / yes |
| Max advertising data | 1,650 bytes | 1,650 bytes |
| Offloaded filtering / scan batching | yes / yes | yes / yes |
| ATT MTU negotiated | 517 | 517 |

### Runs (the 9a as client, 150 bytes each way, every echo checked byte for byte)

| Run | GATT ok / fail | GATT median | L2CAP ok / fail | L2CAP median | Service survived |
| --- | --- | --- | --- | --- | --- |
| Screen on, app in front, 3 min | 3 / 0 | 119 ms | 3 / 0 | 37 ms | yes |
| Background, screen off, unplugged and forced Doze, 15 min | 13 / 2 | 121 ms | 15 / 0 | 37 ms | yes |
| Background, screen off, unplugged, no forced Doze, 15 min | 10 / 4 | 138 ms | 14 / 0 | 42 ms | yes |

- Discovery took 175 ms (9a) and 261 ms (11 Pro) after the service started, with legacy advertising on the 1M PHY.
- One L2CAP channel opened in 363 ms and carried all 33 rounds. GATT reconnects took 158 to 814 ms.
- The six GATT failures were "notify timeout": the server got the write, and the client's probe dropped a notify that arrived before the write confirmation. This is a defect in the spike, not the radio, so the GATT failure count is not evidence; its round trips are.
- No status code other than clean disconnects.

### Decisions

- **Data path: L2CAP CoC**, with GATT as the fallback for a phone or a peer where a channel does not open. L2CAP was three times faster, kept one channel open through Doze, and needs no MTU dance.
- **Advertising: legacy for discovery.** It found the peer in under 300 ms and fits the service UUID, a rotating id and the PSM. Extended advertising is on both Pixels but not every phone, so it is not required. Whether to use it opportunistically (or the Coded PHY for range) is a measurement for the field test (WI-028).

### Still to do here

- The OPPO Find X3 Pro, or another phone with an aggressive battery manager: these two Pixels say nothing about OEM killers, the question this spike exists for.
- A run off the cable: USB and a live adb session can keep a phone out of real deep sleep, so "survived Doze" above is optimistic.
- Spec A1 stays ⚠️ until both are done.

## Review
