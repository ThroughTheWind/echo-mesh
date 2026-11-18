---
id: WI-059
title: Let organisers make rare, signed announcements
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F30, US-39, FR-30.1, FR-30.2, FR-30.3, FR-30.4, AC-42]
epic: WI-052
children: []
---

## Proposal (rationale)

When phones and megaphones fail, a march still needs "moving to point B". A stolen key could misdirect a crowd, so rate, lifetime and the visible signer are the safeguards.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Organiser role sends a fixed phrase with optional pin (FR-30.1).
- Banner to trusting phones with name and fingerprint; hidden from others (FR-30.2).
- At most 1 per 5 minutes per issuer, 30-minute lifetime, enforced by relays (FR-30.3); never merged across organisations (FR-30.4).

### Impacts

- Wire (announcement kind), relay rules, `app` (banner).

### Approach

- Relays enforce the per-issuer rate using the issuer key id inside the credential.

### Acceptance criteria / tests

- AC-42: banner on trusting phones; a second one within 5 minutes is dropped by relays.

### Out of scope

- Free-text announcements: never.

## Execution

## Review
