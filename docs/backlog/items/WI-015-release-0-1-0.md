---
id: WI-015
title: Set up release signing and cut 0.1.0
type: chore
status: planned
branch:
created: 2026-11-17
updated: 2026-11-17
related: [AC-22, NFR-16]
epic: WI-006
children: []
---

## Proposal (rationale)

The release workflow exists but has never run. 0.1.0 is the first build people install, so the signing key must exist and the workflow must be proven before the tag.

Phase 1 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Antoine creates the release keystore and the four `RELEASE_*` secrets (`docs/releasing.md`).
- A dry run of the release workflow on `main` signs and verifies the APK.
- `docs/releases/0.1.0.md` written, saying plainly what 0.1.0 does not protect yet; changelog cut; tag pushed by Antoine.

### Impacts

- `docs/releases/`, `CHANGELOG.md`, `gradle/libs.versions.toml` version. Use the `/cut-release` skill.

### Approach

- Follow `docs/releasing.md`; the agent never pushes the tag itself.

### Acceptance criteria / tests

- AC-22 (release part): the tag `v0.1.0` produces a signed release APK and checksum on GitHub Releases.
- The APK installs on each reference phone.

### Out of scope

- F-Droid and reproducible-build verification: WI-032.

## Execution

## Review
