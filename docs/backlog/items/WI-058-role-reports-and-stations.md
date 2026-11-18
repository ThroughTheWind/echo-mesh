---
id: WI-058
title: Show role badges, stations and role confirmations
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F29, US-37, US-38, FR-29.2, FR-29.2a, FR-29.4, FR-29.5, AC-40]
epic: WI-052
children: []
---

## Proposal (rationale)

A role only helps if trusting phones can see it on reports, find medic stations, and weigh marshal confirmations.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Role reports carry the credential and show "<Role> · issued by <name>" to trusting phones, ordinary to others (FR-29.2).
- Per-report choice to sign as the role (FR-29.2a).
- Station pins for medics and legal observers (FR-29.4).
- Trusted role confirmations count in FR-6.4 and FR-15.3 (FR-29.5); role reports can trigger alerts (WI-043).

### Impacts

- Wire (role report kind), `app` (badge, station pin), vote rules, design components.

### Approach

- Extend the report frame per the accepted ADR; new kind, versioned and fuzzed.

### Acceptance criteria / tests

- AC-40: badge and issuer on a trusting phone, plain public on a non-trusting one.

### Out of scope

- Announcements (WI-059).

## Execution

## Review
