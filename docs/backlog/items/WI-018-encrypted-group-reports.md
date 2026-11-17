---
id: WI-018
title: Send encrypted group reports that everyone relays and only members read
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [F9, FR-5.4, FR-9.1, AC-13, AC-14, AC-16]
epic: WI-016
children: []
---

## Proposal (rationale)

Groups benefit from the whole crowd only if non-members relay their traffic without being able to read it or tell which group it belongs to.

Phase 2 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Group reports end-to-end encrypted to the group and authenticated as from a member (FR-9.1).
- Fixed-size frames, no group id on the air, checkable by non-members before relaying (FR-5.4, threat model §5.3).
- Ratcheted group key as ADR-0004 says; after leave or expiry old reports cannot be read (AC-16).

### Impacts

- `core/crypto`, `core/mesh`, the wire format (a new frame kind, versioned per WI-003).

### Approach

- Reuse the relay of WI-009 unchanged: group frames are just another validated frame kind.

### Acceptance criteria / tests

- AC-13: a group report from A shows on B in the verified style.
- AC-14 (L): a phone outside the group relays it but cannot read it.
- AC-16: after leaving or expiry the group's keys are gone and its reports cannot be read.
- The fuzz target covers the group frame.

### Out of scope

- Votes: WI-019. Position sharing: WI-020.

## Execution

## Review
