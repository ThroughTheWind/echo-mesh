---
id: WI-027
title: Audit storage and radio for leaks
type: chore
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [AC-18, AC-19, NFR-4, NFR-5]
epic: WI-023
children: []
---

## Proposal (rationale)

Two acceptance criteria are checks of the artifact, not the code: what is on disk after use, and what is on the air over half an hour.

Phase 3 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- AC-18: inspect a debug copy of app storage after a realistic session; no plaintext reports, positions, keys or group names.
- AC-19: a 30-minute BLE capture (open-source sniffer) shows no app token or address surviving a 10-minute rotation.
- Write the procedure down so it can be repeated before each release.

### Impacts

- A procedure under `docs/`; any leak found becomes its own item.

### Approach

- Run on two reference phones, the OPPO included.

### Acceptance criteria / tests

- AC-18 and AC-19 pass, with the evidence (file listing, capture summary) recorded in Execution.

### Out of scope

- Fixing leaks found: new items, not this one.

## Execution

2026-11-19, the written half, while the phones are away.

- The procedure is [docs/audits.md](../../audits.md), linked from the release steps in `docs/releasing.md` so it runs before each release: AC-18 pulls a debug build's storage with `run-as` after a scripted 20-minute session, lists it (only `files/sealed/` and `files/maps/` may exist) and scans it; AC-19 captures BLE advertising for 30 minutes with an open-source sniffer and checks addresses, tokens and the device name.
- `scripts/audit/storage_scan.py` does the scan: each name in UTF-8 and UTF-16, each coordinate in wire units, as doubles and as decimals. It refuses to run with no needle, reports an empty directory as blind rather than passing, and has a self-test with a planted leak.
- On every build, `GroupRecordTest` and `PositionSharingTest` already check that a group record holds no position and that only group records are kept.

Still to do on this item: run AC-18 and AC-19 on two reference phones, the OPPO included, and record the evidence here.

## Review
