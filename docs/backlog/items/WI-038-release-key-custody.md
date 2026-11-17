---
id: WI-038
title: Publish who signs Echo and how to check it
type: docs
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [F19, US-26, FR-19.1, FR-19.2, FR-19.3, FR-19.4, FR-19.5, NFR-24, AC-30]
epic: WI-029
children: []
---

## Proposal (rationale)

A coerced or fake update is the most direct attack on people who trust Echo. The signing key's custody and fingerprint must be public and checkable.

Phase 4 of the [roadmap](../../ROADMAP.md), from the [extended spec](../../SPEC-EXTENDED.md)'s 1.0 additions.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the extended spec's 1.0 additions ([SPEC-EXTENDED.md](../../SPEC-EXTENDED.md)).

## Plan

### Requirements

- Antoine holds the release key, stored offline with an encrypted offline backup; the procedure is in `docs/releasing.md` (FR-19.1).
- The signing certificate's SHA-256 fingerprint is in the README, `SECURITY.md`, every release note, and the About screen as text and QR code (FR-19.2).
- `SECURITY.md` names a private reporting route, a response target and the disclosure process, and states the single-key-holder risk (FR-19.3, FR-19.5).

### Impacts

- `docs/releasing.md`, `SECURITY.md`, `README.md`, release notes template, `app` (About screen), CI.

### Approach

- The fingerprint is generated, not typed: CI derives it from the signed APK and checks every place it appears (AGENTS.md, claims and numbers).

### Acceptance criteria / tests

- AC-30: the fingerprint shown in About matches the README, `SECURITY.md` and the release notes, checked by CI on the release workflow.

### Out of scope

- A second key holder or co-signing: revisit before 2.0 (FR-19.5).
- Reproducible builds: WI-032.

## Execution

## Review
