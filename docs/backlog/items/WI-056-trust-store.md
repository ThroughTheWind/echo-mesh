---
id: WI-056
title: Let users choose which organisations they trust
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F28, US-36, FR-28.1, FR-28.2, FR-28.3, FR-28.4, FR-28.5]
epic: WI-052
children: []
---

## Proposal (rationale)

Unions, associations and media can only be recognised if each user decides whom to trust; no built-in list, no Echo authority.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Organisation profile verified against its root key; issuer keys valid at most 7 days (FR-28.1, FR-28.2).
- Add in person by QR or from an accepted pack, fingerprint always shown, never transitive (FR-28.3).
- Encrypted, removable, wiped (FR-28.4); signed revocations spread and are honoured (FR-28.5).

### Impacts

- `core/crypto` (verification, Tink only), `core/data`, `app` (trust store screen), wire (revocation kind).

### Approach

- Per the accepted organisation credentials ADR (WI-053).

### Acceptance criteria / tests

- A trusted organisation's issuer verifies; an untrusted or expired one doesn't; a revocation propagates in the simulator.
- NFR-20 fuzzing of credentials and revocations.

### Out of scope

- Issuing roles (WI-057).

## Execution

## Review
