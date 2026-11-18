---
id: WI-057
title: Issue role credentials in person
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F29, FR-29.1, FR-29.3, FR-29.6, AC-41]
epic: WI-052
children: []
---

## Proposal (rationale)

Marshals and medics must be issued a role on the day, in person, for at most 24 hours, knowing what it exposes.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Issuer scans the holder's QR and signs a credential with org fingerprint, role, validity at most 24 h, per-event key (FR-29.1).
- The issuance screen states linkability; press carries an extra warning (FR-29.3).
- Marshal and medic first; press last (FR-29.6).

### Impacts

- `core/crypto`, `app` (issuer and holder flows), copy traced to threat model §7.

### Approach

- An issuer mode in the app for people holding an issuer key.

### Acceptance criteria / tests

- AC-41: a credential past 24 h, or revoked, is rejected.
- Issuance works offline between two reference phones.

### Out of scope

- Showing badges on reports (WI-058).

## Execution

## Review
