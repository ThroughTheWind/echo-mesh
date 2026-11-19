---
id: WI-036
title: Export a field diagnostics file with no positions or reports
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
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

2026-11-19.

- `core/mesh/diagnostics/FieldDiagnostics.kt` counts, by minute since diagnostics were turned on: distinct peer handles (held for the current minute only, then just the count), reports received from other phones, frames relayed, link failures by kind, battery every 10 minutes, and minutes when Android stopped the mesh. Delivery delays are a histogram of whole minutes from creation to arrival. `DiagnosticsFile` writes it as plain text in four small CSV tables whose headers are a closed set.
- Link failures needed the transport to say so: `TransportEvent.LinkFailed(kind)` with `LinkFailure` (session start, connect, hello, link broken), naming the kind, never the phone. The BLE adapter sends it at its four failure points; the relay passes it to the counters and does nothing else with it.
- `MeshNode.startDiagnostics` / `stopDiagnostics` / `diagnosticsExport`; the panic wipe stops them. Only the switch is kept, in sealed settings; the counts start again at each launch. A launch that finds the mesh still meant on records `killed_before_session,yes` (the process was killed). The service ending while the mesh is meant on and running counts as `service_killed`; `meshOff` now forgets "meant on" first so a user's own stop is not counted.
- App: a "Field diagnostics" row in the mesh sheet opens a screen with the switch, what it counts and never records, and while on the whole file in monospace with Share and Refresh. Share goes through `DiagnosticsProvider`, a non-exported content provider that pipes the reviewed text from memory, so nothing is written to storage even at export, and the storage audit stays clean.
- Tests: `FieldDiagnosticsTest` (per-minute peers with no token in the file, the sums, battery every 10 minutes, NFR-19's line-by-line shape check, an empty session) and `DiagnosticsMeshTest` on loopback (a session with a public report, a group named and nicknamed, a group report and a shared position exports no name, coordinate, type or date; off and the wipe forget it). `BleTransportTest` checks the broken-link event.
- `docs/field-testing.md` is the protocol (FR-17.5). Design: `screens/FieldDiagnostics`, and the mesh sheet's row in `components/MeshStatus`.
- Deviations: "reports received" counts new public reports and new group frames (a group frame's content is sealed, so a vote or position inside one counts too), and delivery delay is measured on public reports only, whose creation minute is readable. There is no settings page yet, so the switch lives behind the mesh sheet.

Still to do on this item: AC-28 on phones, with the field test (WI-028).

branch-merged-ok: the code merges with PR #6 while AC-28 waits for the field test.

## Review
