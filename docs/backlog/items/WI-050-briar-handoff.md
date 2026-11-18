---
id: WI-050
title: Hand off to Briar for chat
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F27, US-35, FR-27.1, FR-27.2]
epic: WI-039
children: []
---

## Proposal (rationale)

Chat is out of scope on purpose; pointing to Briar covers the need without growing Echo's attack surface.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- "Chat in Briar" on a roster entry opens Briar's add-contact flow if installed (FR-27.1).
- Echo sends and stores nothing about the link; without Briar, the F-Droid page opens in the browser (FR-27.2).

### Impacts

- `app` only; no new permission.

### Approach

- An Android intent to Briar if one exists; otherwise a plain explanation of how to add a contact in Briar.

### Acceptance criteria / tests

- Manual test with Briar installed and not installed; no data written.

### Out of scope

- Any Briar integration beyond opening it; nothing deferred.

## Execution

## Review
