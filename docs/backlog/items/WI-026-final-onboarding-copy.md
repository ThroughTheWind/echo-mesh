---
id: WI-026
title: Final onboarding and "What this app protects" copy, traced to the threat model
type: docs
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [F1, FR-1.1, FR-1.3, FR-3.4a, NFR-14]
epic: WI-023
children: []
---

## Proposal (rationale)

The app must never claim more safety than it delivers. Every sentence that makes a safety claim must trace to a row of threat model §7, so the security review can check it.

Phase 3 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Final copy for onboarding and the settings page, each claim citing its §7 row.
- The residual risk of FR-3.4a (the constant service UUID) is stated.
- A check (test or gate) that every claim string has a §7 reference.

### Impacts

- `app` strings, `docs/threat-model.md` §7 if a row is missing.

### Approach

- Antoine reviews the wording; the design thread's onboarding components carry it.

### Acceptance criteria / tests

- Every safety claim in the UI maps to a §7 row, and the check fails when one does not.

### Out of scope

- Translations: not planned (NFR-17).

## Execution

2026-11-19.

- Threat model §7's rows now have ids, P1 to P6 and N1 to N6, with one sentence saying the app's strings cite them. Meaning unchanged.
- Every safety sentence in `strings.xml` cites its row in the comment just above it: onboarding's protects and doesn't-protect lines, every line of What Echo protects, and the other sentences that make a safety promise (the welcome's "no server", the map download's exposure, report expiry, the share sheet's "nothing is stored", the wipe sheet). 26 strings in all.
- `scripts/gates/claims.mjs`, a fast gate with a self-test, fails when a claim string cites no row, cites a row §7 does not have, or when a row is missing from What Echo protects. It runs in `rungs check`, so in CI.
- The residual risk of FR-3.4a, the constant service UUID, is N1 ("Anyone nearby with a radio receiver can tell your phone is running Echo"), shown in onboarding and on What Echo protects.
- Onboarding's "Before you go" now points at the eye button and the wipe button, which shipped with WI-025 and WI-024.
- Fixed after WI-024 shipped: What Echo protects still left out P5 (the panic wipe), because its screen lists the strings in Kotlin and the gate only read `strings.xml`. The gate now also fails when a page string is not shown by `WhatEchoProtects.kt`, with a self-test case for it, and P5 is listed.
- Deviation: there is no settings page yet, so its copy is not written; when it exists its claims fall under the same gate by citing a row.

Still to do on this item: Antoine reviews the wording (the plan's approach). The copy is faithful to §7 and short; nothing in it is new.

branch-merged-ok: the gate and the copy merge with PR #6 while the wording review stays open on this item.

## Review
