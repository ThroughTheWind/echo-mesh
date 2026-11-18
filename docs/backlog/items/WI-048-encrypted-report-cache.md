---
id: WI-048
title: Cache live reports encrypted so a restart keeps the map
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F25, US-33, FR-25.1, FR-25.1a, FR-25.2, FR-25.3, FR-25.4, FR-25.5, AC-38]
epic: WI-039
children: []
---

## Proposal (rationale)

OEM battery managers (the OPPO class) kill the service, and today that empties the map. A short-lived cache restores it, at the cost of a locked phone holding live reports again.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Entries wrapped to an hourly Keystore public key; the private key works only while unlocked (FR-25.1).
- After a kill while locked, cache and group keys load at next unlock; relaying continues meanwhile (FR-25.1a).
- Crypto-erase at each entry's expiry; positions never cached (FR-25.2).
- Panic wipe destroys the cache key first (FR-25.4).

### Impacts

- `core/data`, `core/crypto` (wrapping via Tink only), FR-12.2 and threat model A3 change (WI-040).

### Approach

- Per-batch data keys wrapped to the hourly public key; deleting the batch key is the erase.

### Acceptance criteria / tests

- AC-38: after a kill, reopening after unlock shows all live reports; entries cached while locked open only after unlock; expired entries unreadable on disk.
- Storage audit (WI-027 method) re-run.

### Out of scope

- Caching positions: never.

## Execution

## Review
