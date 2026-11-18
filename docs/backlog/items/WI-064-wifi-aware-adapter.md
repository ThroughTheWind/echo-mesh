---
id: WI-064
title: Build the Wi-Fi Aware transport adapter
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F33, FR-33.1, FR-33.2, FR-33.3, FR-33.4]
epic: WI-052
children: []
---

## Proposal (rationale)

BLE can't move megabytes. A second adapter behind `MeshTransport` with a bulk capability unlocks maps, packs and app sharing.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- `adapters/wifiaware` with a bulk transfer capability, gated on device support (FR-33.1).
- Tokens rotate with the NAN address; verify before relay (FR-33.2).
- Permission and Location Mode caveats in context (FR-33.3).
- Identifier captures on supporting reference phones (FR-33.4).

### Impacts

- New module, `MeshTransport` capability addition (ADR-0002), boundary gate, manifest gate per WI-063's ADR.

### Approach

- Per the spike's ADR; BLE remains the baseline.

### Acceptance criteria / tests

- Adapter tests on the loopback harness; identifier captures show no stable id.

### Out of scope

- Using it for app, map and pack sharing (WI-065, WI-066).

## Execution

## Review
