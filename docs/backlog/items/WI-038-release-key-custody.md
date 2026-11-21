---
id: WI-038
title: Publish who signs Echo and how to check it
type: docs
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
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

2026-11-19.

- The fingerprint is kept once, in `docs/signing-certificate.sha256`, copied from the first signed dry run. The release workflow refuses an APK whose certificate differs from it, refuses a tag while it is missing, and on a dry run prints the certificate to copy. The release notes already carried it.
- `scripts/gates/fingerprint.mjs` (fast tier, with a self-test): README.md and SECURITY.md show the fingerprint between `signing-fingerprint` markers, and it must be the file's; before the first release both say "No signed release yet".
- README gains "Checking that your copy is ours"; SECURITY.md gains the same block, how to check an APK with `apksigner`, and "One person holds the key" (FR-19.5). Its private route, 7-day acknowledgement and disclosure process were already there (FR-19.3).
- `docs/releasing.md` gains step 4 (publish the fingerprint) and "Keeping the key": master copy on an encrypted offline drive, an encrypted offline backup elsewhere, the password in a password manager, and what to do if the key is lost or exposed.

Open, for Antoine:

- ~~FR-19.1 versus the CI copy of the key~~: decided by Antoine on 2026-11-19, the CI copy stays. FR-19.1, `docs/releasing.md` and SECURITY.md now say so.
- The key itself does not exist yet (WI-015), so the fingerprint cannot be published until then.
- The fingerprint in Settings > About, as text and QR: the settings screen exists (WI-074); the row waits for the key.

branch-merged-ok: the gate and docs merge with PR #6 while the key and the About fingerprint row wait.

## Review
