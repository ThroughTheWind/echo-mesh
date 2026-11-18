---
id: WI-053
title: Accept the organisation credentials design and the threat model's 2.0 section
type: docs
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F28, F29, F30, F31, F32, F34]
epic: WI-052
children: []
---

## Proposal (rationale)

Organisation keys, issuers, role credentials, revocation, crisis groups and app sharing are new trust surfaces. The threat model thread drafted them (threat model §12 and an organisation credentials ADR); they must be accepted before code.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Threat model §12 and its §7 claim rows are in the repo.
- The organisation credentials ADR is merged with an unused number and accepted by Antoine.
- Each 2.0 feature with **[DEP: threat]** or **[DEP: ADR]** links its answer.

### Impacts

- `docs/threat-model.md`, `docs/decisions/`, `docs/SPEC-EXTENDED.md` §10.

### Approach

- The threat model thread's draft names its ADR ADR-0005, which is already the wire format; it takes the next free number when it lands.

### Acceptance criteria / tests

- Antoine accepts the ADR in writing; `node .ai/rungs.mjs check` passes.

### Out of scope

- The outside review itself (WI-054).

## Execution

## Review
