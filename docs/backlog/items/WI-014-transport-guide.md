---
id: WI-014
title: Write docs/transports.md: how to add a transport
type: docs
status: review
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [FR-14.4, ADR-0002]
epic: WI-006
children: []
---

## Proposal (rationale)

FR-14.4 asks for a guide, and the first real adapter is the moment its lessons are fresh. Zapper's `docs/adapters.md` is the model.

Phase 1 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- How to create `:adapters:<radio>`, declare capabilities honestly, pass `MeshTransportContract`, register, and what the gates check.
- The per-phone radio facts from WI-007 and WI-011.

### Impacts

- `docs/transports.md`, routing in `AGENTS.md`, `CONTRIBUTING.md`.

### Approach

- Written from the BLE adapter as the worked example.

### Acceptance criteria / tests

- `docs/transports.md` exists, is linked from `AGENTS.md` and `CONTRIBUTING.md`, and `gates-links-resolve` passes.
- Every command in it runs as written (`instructions-stale-commands` passes).

### Out of scope

- Final review of the guide before 1.0: WI-033.

## Execution

2026-11-19. [`docs/transports.md`](../../transports.md) covers what a transport is and is not, the contract, nothing stable on the air, honest capabilities, nine steps from module to phones with the BLE adapter as the worked example, what each gate checks, and the radio facts measured on the Pixel 9a, Pixel 11 Pro and OnePlus Nord CE 3 Lite (WI-007, WI-011). Linked from the routing table in `AGENTS.md` and from `CONTRIBUTING.md`; `gates-links-resolve` and `instructions-stale-commands` pass.

## Review
