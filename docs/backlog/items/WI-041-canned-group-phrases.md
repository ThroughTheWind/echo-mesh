---
id: WI-041
title: Send canned phrases inside a group
type: feature
status: planned
branch:
created: 2026-11-18
updated: 2026-11-18
related: [F20, US-27, FR-20.1, FR-20.2, FR-20.3, FR-20.4, AC-32]
epic: WI-039
children: []
---

## Proposal (rationale)

Groups need to say "regroup", "I'm leaving" or "need help" without chat, and free text is excluded on purpose. A fixed phrase set covers most of the need at report size.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the mature-version assessment and the [extended spec](../../SPEC-EXTENDED.md), then asking to turn it into phased work items.

## Plan

### Requirements

- Phrases from FR-20.1, some with a pin, sent as a new payload type inside the existing group frame (FR-20.2), default expiry 15 minutes.
- No free text; labels come from the catalog so translations change words, not meaning (FR-20.3).
- A group feed, plus pins on the map; memory-only (FR-20.4).

### Impacts

- `core/model` (phrase type), `core/mesh` wire (new group payload kind, versioned per ADR-0005), `app` (feed, composer), fuzz corpus.

### Approach

- Reuse the group frame and its fixed size; add the payload kind and extend the decoder fuzz target in the same change.

### Acceptance criteria / tests

- AC-32: a phrase from A shows in B's feed within 10 s; a non-member relays it and can't read it.
- Fuzz target covers the new payload kind.

### Out of scope

- Rally point (WI-042), check-ins (WI-044), announcements (WI-059).

## Execution

## Review
