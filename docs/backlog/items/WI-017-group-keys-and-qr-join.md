---
id: WI-017
title: Create groups and join them in person by QR
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [F8, FR-8.1, FR-8.2, FR-8.3, FR-8.5, FR-8.6, FR-12.5, AC-13, AC-25, ADR-0004]
epic: WI-016
children: []
---

## Proposal (rationale)

Group keys must never cross the mesh. They travel in a Noise handshake over one direct hop after an in-person QR scan, and a photographed QR is worth at most one join.

Phase 2 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Create a group with a local name and lifetime (FR-8.1); keys generated in `:core:crypto`.
- QR with a one-time join key and token; open-source in-app scanner, no Play Services (FR-8.2).
- Noise handshake over one direct radio hop; every join visible to the group.
- Leave deletes keys; expiry deletes keys on every phone (FR-8.5, FR-8.6). QR screen uses `FLAG_SECURE`.

### Impacts

- `core/crypto`, `core/mesh` (direct-hop channel), `core/data`, `app`. A QR library must pass the dependency gate.

### Approach

- Follow ADR-0004 and threat model §5.5; tests for the handshake run on loopback with an attacker in the middle.

### Acceptance criteria / tests

- AC-25: scanning the same QR twice fails the second time, and the first join was shown to the group.
- The join handshake rejects a replayed or tampered message in tests.

### Out of scope

- Group report encryption: WI-018. Roster and join screens: WI-022.

## Execution

## Review
