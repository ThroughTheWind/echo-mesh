---
id: WI-032
title: Verify the build is reproducible and F-Droid ready
type: chore
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [AC-31, NFR-24, FR-19.4, NFR-16, ADR-0003]
epic: WI-029
children: []
---

## Proposal (rationale)

Anyone should be able to rebuild the APK and get the same bytes, which is what makes the "no tracker" claim checkable and what F-Droid needs.

Phase 4 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Two independent builds of the same tag produce identical unsigned APKs (or the differences are explained and removed).
- F-Droid metadata drafted; inclusion itself is after 1.0.

### Impacts

- Build files, CI (a reproducibility job).

### Approach

- Follow F-Droid's reproducible-builds guidance.

### Acceptance criteria / tests

- A CI job builds twice and compares, and fails on a difference.
- AC-31 and NFR-24: an independent rebuild of a release produces the same APK, verified before publishing, with the steps documented (FR-19.4).

### Out of scope

- Submitting to F-Droid: after 1.0.

## Execution

2026-11-19.

- `scripts/reproducible.sh` builds the unsigned release APK of a commit twice, from two worktrees at different paths, in different time zones and locales, and fails on any difference: first file by file (CRC, size and name, so the report names what differs), then byte for byte. A comparison that lists no files fails rather than passing blind.
- With `--against <apk>` it builds once and compares with a published, signed APK, the signature files apart. The release workflow runs it on the tag before publishing; `docs/releasing.md` says how anyone runs the same check.
- A CI job, `reproducible`, runs the two-build comparison on every pull request. Its first run gave two identical builds.
- `fastlane/metadata/android/en-US/` holds the store text and `docs/fdroid/` a draft F-Droid metadata file; its signing key line waits for the release key (WI-038). Submitting stays after 1.0.

## Review
