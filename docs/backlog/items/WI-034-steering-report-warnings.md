---
id: WI-034
title: Warn on public steering reports and hide their public counts
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [F15, US-22, FR-15.1, FR-15.2, FR-15.3, FR-15.5, AC-26]
epic: WI-029
children: []
---

## Proposal (rationale)

A fake public "Exit open" or "All clear" can lead people into a kettle. Steering reports stay public, but must look unconfirmed and must not borrow credibility from public counts that a flooder can mint.

Phase 4 of the [roadmap](../../ROADMAP.md), from the [extended spec](../../SPEC-EXTENDED.md)'s 1.0 additions.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the extended spec's 1.0 additions ([SPEC-EXTENDED.md](../../SPEC-EXTENDED.md)).

## Plan

### Requirements

- The catalog marks steering types: Exit open, All clear here, Street closed (FR-15.1).
- Public steering reports from strangers carry the "Unconfirmed — could be a trap" style on the pin, in the list and in the detail (FR-15.2).
- No public confirmation count on public steering reports; group members' confirmations still show (FR-15.3). The "probably gone" rule is unchanged (FR-15.4).
- The warning copy traces to a new row of threat model §7 (FR-15.5).

### Impacts

- `core/model` (catalog flag), `app` (pin, list, detail), `docs/design/` (warning style), `docs/threat-model.md` §7.

### Approach

- A flag on the report type, read by the UI; no wire change, since the type code already says which type it is (ADR-0005).

### Acceptance criteria / tests

- AC-26: a public "Exit open" from a stranger shows the steering warning and no public confirmation count; a group member's confirmation shows.
- A UI test per steering type, and one for a non-steering type that still shows its public count.

### Out of scope

- Trusted-role confirmations (F29, 2.0).

## Execution

2026-11-19.

- `ReportType.steering` marks Exit open, All clear here and Street closed (FR-15.1); no wire change.
- Threat model §7 gains **N7**: a public report saying where to go, or that a place is safe, can be a trap; the app marks it unconfirmed and hides its public confirmations (FR-15.5). What Echo protects lists it (the claims gate requires it), and the detail's warning cites it.
- `core/mesh/node/Steering.kt`: `couldBeATrap` (a public steering report not from this phone), `publicConfirmationsHidden` (every public steering report) and `confirmationsShown` (group confirmations, plus public ones unless hidden). The screens read only these, so the rule is tested once in core.
- Pin: an amber "?" badge bottom right and "unconfirmed, could be a trap" read aloud; the count badge shows group confirmations only. List: a warning support line. Detail: an amber callout above the provenance box, and the nearby votes show dismissals only. "Probably gone" is unchanged (FR-15.4).
- Tests: `SteeringTest` covers AC-26 for each steering type, a non-steering type keeping its public count, a group's steering report and your own; `ReportTest` pins the steering set.
- Deviation: the plan's "UI test per steering type" is a core test per type instead, since the screens only render what `Steering.kt` decides; this environment runs no instrumented tests. Design aligned: ReportPin, ReportDetail, ReportsNearYou, WhatEchoProtects.

Still to do on this item: AC-26 on phones, and Antoine reviews the N7 wording with WI-026's copy review.

branch-merged-ok: the code merges with PR #6 while AC-26 waits for phones.

## Review
