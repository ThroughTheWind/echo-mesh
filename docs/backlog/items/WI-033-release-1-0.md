---
id: WI-033
title: Final docs and cut 1.0.0
type: chore
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [NFR-16, FR-14.4]
epic: WI-029
children: []
---

## Proposal (rationale)

The release that tells people they may rely on Echo within its stated limits.

Phase 4 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- `SECURITY.md`, `README.md` and `docs/transports.md` final; the README no longer says pre-alpha.
- Release notes summarise the security review and what Echo does not protect.
- Version 1.0.0 cut with `/cut-release`; Antoine pushes the tag.

### Impacts

- Docs, `CHANGELOG.md`, `docs/releases/1.0.0.md`, version catalog.

### Approach

- Only after WI-030 to WI-032 are done.

### Acceptance criteria / tests

- The `v1.0.0` release is published with APK and checksum, and installs over 0.3.0 on every reference phone.

### Out of scope

- Anything after 1.0: separate items.

## Execution

## Review
