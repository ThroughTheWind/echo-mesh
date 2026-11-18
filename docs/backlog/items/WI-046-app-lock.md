---
id: WI-046
title: Lock the app with a PIN and an attempt limit
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F24, US-32, FR-24.1, FR-24.4, FR-24.5, FR-24.6, AC-37]
epic: WI-039
children: []
---

## Proposal (rationale)

A phone taken unlocked exposes groups in seconds. An app lock covers that case; it does not stop forensic tools, and the copy must not claim it does.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Opt-in lock after a chosen background time, opened by an app PIN or the device credential; the mesh keeps relaying (FR-24.1).
- Hard limit on wrong attempts with optional panic wipe (FR-24.5).
- Copy never claims protection against extraction (FR-24.6); off by default (FR-24.4).

### Impacts

- `app` (lock screen, settings), `core/data` (PIN verifier), threat model §7 copy.

### Approach

- PIN verified against a salted hash in the encrypted store; attempt counter survives restarts.

### Acceptance criteria / tests

- AC-37 (lock part): the app asks for the PIN after the timeout; the limit triggers the chosen action.

### Out of scope

- Duress PIN and auto-wipe (WI-047).

## Execution

## Review
