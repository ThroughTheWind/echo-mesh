---
id: WI-063
title: Prove Wi-Fi Aware on the reference phones and settle the INTERNET question
type: spike
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F33, ADR-0003]
epic: WI-052
children: []
---

## Proposal (rationale)

Wi-Fi Aware coverage is patchy, and a socket over its data path needs the `INTERNET` permission, which ADR-0003 forbids by default. Both facts decide whether F33 and F34 are possible as specified.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Record which reference phones support Wi-Fi Aware and how many data paths.
- Measure transfer of a 50 MB file, discovery time and NAN address rotation.
- Decide in an ADR how bulk transfer meets ADR-0003: a confined `INTERNET` permission enforced by the manifest gate, or no data path (discovery messages only).

### Impacts

- `docs/research/`, a new ADR possibly amending ADR-0003's manifest rule, the manifest gate.

### Approach

- A throwaway spike app or test build on the reference phones, then the ADR.

### Acceptance criteria / tests

- Measurements written up; ADR accepted by Antoine.

### Out of scope

- The adapter itself (WI-064).

## Execution

## Review
