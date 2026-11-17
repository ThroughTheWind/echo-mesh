---
id: WI-030
title: Independent security review and fixes
type: chore
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [NFR-8, ADR-0004, SECURITY.md]
epic: WI-029
children: []
---

## Proposal (rationale)

NFR-8 and SECURITY.md promise that 1.0 does not ship without an outside review of the protocol, the crypto use and the wire decoder. Threat model §8 lists what it must cover.

Phase 4 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Find a reviewer (for example an audit programme for open-source privacy tools); share the threat model, ADRs and code.
- Every finding gets a work item or a written reason it is accepted.
- Publish the review summary.

### Impacts

- Potentially any module; fixes are their own items.

### Approach

- Antoine owns the reviewer relationship; the agent prepares the review package.

### Acceptance criteria / tests

- A written review exists covering threat model §8; every high or critical finding is fixed and re-checked.

### Out of scope

- Fixes themselves: one item per finding.

## Execution

## Review
