---
id: WI-051
title: Cut 1.1.0
type: chore
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [AC-32, AC-39, NFR-24]
epic: WI-039
children: []
---

## Proposal (rationale)

Ship Phase 5 to the reference phones and GitHub Releases.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Every Phase 5 item done; AC-32 to AC-39 re-run on the candidate.
- Release notes list the new claims from threat model §7 and the fingerprint (FR-19.2).

### Impacts

- CHANGELOG, release notes, `docs/releasing.md`.

### Approach

- Same procedure as 1.0.0 (WI-033), with a reproducibility check (NFR-24).

### Acceptance criteria / tests

- The 1.1.0 release page has the APK, checksum and fingerprint; an independent rebuild matches.

### Out of scope

- Nothing deferred.

## Execution

## Review
