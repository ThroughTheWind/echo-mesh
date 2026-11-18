---
id: WI-065
title: Share Echo itself offline with an authenticity check
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F34, US-42, FR-34.1, FR-34.2, FR-34.3, FR-34.4, AC-46]
epic: WI-052
children: []
---

## Proposal (rationale)

During a shutdown GitHub is out of reach; Echo must spread phone to phone, and a fake must be catchable.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Share the installed APK via share sheet or Wi-Fi Aware (FR-34.1).
- Fingerprint as code and QR; first launch shows it before anything else; copy asks to compare with an independent source (FR-34.2).
- Copy explains Android refuses differently signed updates (FR-34.3).

### Impacts

- `app`, Wi-Fi Aware bulk (WI-064), threat model A5 (WI-053).

### Approach

- Share sheet first (works without Wi-Fi Aware), then Wi-Fi Aware.

### Acceptance criteria / tests

- AC-46: shared APK installs on a phone without Echo and shows the same fingerprint; install over a differently signed Echo fails.

### Out of scope

- Delta updates or update notifications; nothing deferred.

## Execution

## Review
