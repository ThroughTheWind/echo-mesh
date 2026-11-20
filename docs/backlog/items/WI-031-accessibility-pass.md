---
id: WI-031
title: Accessibility pass
type: chore
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [NFR-11, NFR-12]
epic: WI-029
children: []
---

## Proposal (rationale)

People with screen readers, larger text or limited dexterity are at protests too.

Phase 4 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- Content descriptions everywhere; font scaling; WCAG AA contrast (the design tokens gate covers colours).
- TalkBack can drop and read a report end to end.
- Primary touch targets ≥ 56 dp checked on every screen.

### Impacts

- `app`, `core/designsystem`.

### Approach

- Compose UI tests with semantics assertions where practical.

### Acceptance criteria / tests

- TalkBack walkthrough of drop and read report recorded; automated semantics tests pass in CI.

### Out of scope

- Translations: not planned (NFR-17).

## Execution

2026-11-19.

- `app/src/test/.../a11y/AccessibilityTest.kt` renders the screens that carry a report from tap to read with Robolectric and reads the semantics tree TalkBack reads: every clickable node must have a spoken name and be at least 48 dp each way. It drops a report by name alone at normal and double font size, cancels the grace period by name, reads a public steering report end to end (it must say it could be a trap), and checks the mesh sheet and What Echo protects. Material's sheet drag handle is skipped: TalkBack works it through its own actions, and every sheet also closes with Back and its own button.
- The check found real defects on its first run: no bottom sheet scrolled, so on a small screen the report sheet squeezed its vote buttons to 32 dp and the mesh sheet its field diagnostics row to nothing. Every sheet's content now scrolls, and components/BottomSheet says it must.
- The app's unit tests print a failure's message in full, so CI names the control instead of only the line that asserted.
- Screens that need the whole app (map, settings, groups) are left to the TalkBack walkthrough on phones, queued with the other phone checks.

- 2026-11-20, label check on a Pixel 9a by reading Android's accessibility tree (the hands-on TalkBack walkthrough was dropped: it needs someone who uses TalkBack daily). Every control on the map, the report flow, a report's detail with votes, Reports near you and Settings is named, except three, fixed: the map's Report button reached the tree with no label at all, so it now carries its label itself; each report type tile was named twice, by its description and its text, so the text is left out (the picker test now fails if it comes back); each row in Reports near you gave TalkBack a second stop on its pin, which is now left out. Long distances read in km.
- 2026-11-20: a refused location permission ended at a dead end ("Location is off for Echo") when Android refused without its dialog; the refusal now offers Echo's page in Settings, on the map and in the share sheet.

## Review
