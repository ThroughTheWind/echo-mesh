---
id: WI-036
title: Export a field diagnostics file with no positions or reports
type: feature
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [F17, US-24, FR-17.1, FR-17.2, FR-17.3, FR-17.4, FR-17.5, NFR-19, AC-28]
epic: WI-023
children: []
---

## Proposal (rationale)

Tuning lifetimes, rates, thresholds and battery needs data from real crowds, and Echo has no telemetry (ADR-0003). A file the user exports by hand, showing everything it contains first, is the only honest route.

Phase 3 (the field test WI-028 uses it) of the [roadmap](../../ROADMAP.md), from the [extended spec](../../SPEC-EXTENDED.md)'s 1.0 additions.

## Decision

`accepted` — 2026-11-17, Antoine, by accepting the extended spec's 1.0 additions ([SPEC-EXTENDED.md](../../SPEC-EXTENDED.md)).

## Plan

### Requirements

- Off by default. When on, counts in memory only: distinct peer tokens per minute, reports received and relayed per minute, delivery delay from creation time, connection failures by kind, battery level every 10 minutes, and whether the foreground service was killed (FR-17.1).
- Never records positions, report contents or types, map areas, group names, nicknames, keys or tokens (FR-17.2).
- Exported by hand through the share sheet, after a screen showing the whole file as plain text; nothing is sent automatically (FR-17.3).
- Wiped by the panic wipe and when turned off; never written to disk before export (FR-17.4).
- `docs/field-testing.md` gives the field-test protocol (FR-17.5).

### Impacts

- `core/mesh` (counters), `app` (settings, export screen), `core/data` (none: nothing persists), docs.

### Approach

- A plain counter object with a closed set of fields, so NFR-19 is a test of its serialised form.

### Acceptance criteria / tests

- NFR-19: a test asserts the export contains only the FR-17.1 fields.
- AC-28: an export from a field session contains only FR-17.1 fields, and nothing is written to disk before export.

### Out of scope

- Any automatic upload: never (ADR-0003).

## Execution

## Review
