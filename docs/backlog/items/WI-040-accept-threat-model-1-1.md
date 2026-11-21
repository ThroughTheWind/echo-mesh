---
id: WI-040
title: Review and accept the threat model's 1.1 section
type: docs
status: in_progress
branch: feature/WI-040-threat-model-1-1
created: 2026-11-18
updated: 2026-11-21
related: [F20, F24, F25, ADR-0004, ADR-0009, ADR-0010]
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

2026-11-21, started with Phase 5 after 0.2.0's release PR.

- §11 comes from the threat model thread's v0.2 draft (the project folder's `protest-map/docs/threat-model.md`, 2026-11-18), with the 1.1 rows it adds to §2, §4 (A3), §5.3, §8 and §9. The repo's own changes since that draft (P/N row ids, ADR-0005, ADR-0008, `ClockCheck`, the relay budget) are kept; the draft's older wording of those parts is not brought back.
- **Deviation: a new ADR instead of an ADR-0004 amendment.** Accepted records are immutable (docs/decisions/README.md), and the durable-item rule also contradicts ADR-0009, accepted after the draft was written, which signs every inner message over its epoch. [ADR-0010](../../decisions/ADR-0010-durable-group-items-and-report-cache.md) (proposed) carries both 1.1 constructions: durable items as inner types `0x10`-`0x1F` signed without the epoch, which 0.2.0 phones refuse to read and still relay (`Relay` treats every group frame as opaque); and the hourly Keystore RSA-OAEP envelope for the report cache.
- **Deviation: the 1.1 copy is not §7 rows yet.** `scripts/gates/claims.mjs` requires every row on the "What Echo protects" page, so a row for a feature that does not exist would be a false claim in the app. The copy sits under "Planned for 1.1" and becomes a row in each feature's own change.
- Crisis groups (2.0) are cut from §11.4's bucket limit; they are WI-060's.
- The draft's §10 (1.0) never landed either: finding F-011. §10 is a stub that says so.

Still to do: Antoine reviews §11 and ADR-0010 and accepts them in writing.

## Review
