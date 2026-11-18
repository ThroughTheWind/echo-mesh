---
id: WI-040
title: Review and accept the threat model's 1.1 section
type: docs
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F20, F24, F25, ADR-0004]
epic: WI-039
children: []
---

## Proposal (rationale)

Group phrases, the duress PIN, auto-wipe and the report cache change what a seized phone or a group member can learn (threat model A2, A3). The threat model thread drafted §11 and an ADR-0004 amendment; they must be in the repo and accepted before the code that depends on them.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Threat model §11 (1.1) and its §7 claim rows are in `docs/threat-model.md`.
- The ADR-0004 amendment is merged and accepted by Antoine.
- Every 1.1 feature with **[DEP: threat]** in the extended spec points at the section that answers it.

### Impacts

- `docs/threat-model.md`, `docs/decisions/ADR-0004-crypto-design.md`, `docs/SPEC-EXTENDED.md` references.

### Approach

- Bring the threat model thread's v0.2 text in through its own pull request; this item tracks review and acceptance, not authorship.

### Acceptance criteria / tests

- Antoine has accepted the amendment in writing in the ADR.
- `node .ai/rungs.mjs check` passes with the new sections.

### Out of scope

- The 2.0 sections (WI-053).

## Execution

## Review
