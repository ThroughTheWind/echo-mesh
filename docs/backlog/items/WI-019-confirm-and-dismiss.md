---
id: WI-019
title: Confirm or dismiss a report, with votes that spread
type: feature
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-17
updated: 2026-11-19
related: [F6, FR-6.1, FR-6.2, FR-6.3, FR-6.4, FR-6.5, AC-7, AC-8]
epic: WI-016
children: []
---

## Proposal (rationale)

Reports go stale and some are fake. Votes let the crowd correct the map without anyone being able to erase a pin early, and without public counts posing as proof.

Phase 2 of the [roadmap](../../ROADMAP.md).

## Decision

`accepted` — 2026-11-17, Antoine, by asking to turn the [roadmap](../../ROADMAP.md) into executable work items with deliverable phases.

## Plan

### Requirements

- One vote per report per phone; votes authenticated like reports and relayed the same way (FR-6.1, FR-6.2).
- A confirmation extends time left, capped at twice the default lifetime (FR-6.3).
- The "probably gone" rule exactly as FR-6.4 and threat model §5.7; never removes a pin.
- Copy says "N nearby phones confirmed", never "verified" (FR-6.5).

### Impacts

- `core/model`, `core/crypto`, `core/mesh`, `app` (report detail).

### Approach

- Votes are a frame kind of their own; public and group votes reuse WI-008 and WI-018 signing.

### Acceptance criteria / tests

- AC-7: a confirmation from B shows on A within 10 s and extends time left, never beyond twice the default.
- AC-8: dismissals mark a report "probably gone" without removing it.
- Unit tests for every branch of the FR-6.4 rule.

### Out of scope

- Muting a member's votes: WI-021.

## Execution

2026-11-19, built development-first while the phones are away.

- Public votes are the existing `PublicVoteFrame` (WI-008), signed with a fresh one-time key. Group votes are a fourth inner type of the group frame, `0x02`, the public vote's frame code, as [ADR-0009](../../decisions/ADR-0009-group-frames-and-join.md) provides (`target id 32 ‖ vote 1 ‖ created u32 ‖ lifetime u16`), signed by the member key like a group report.
- `VoteRule` in `core/mesh/report/Votes.kt` is the whole rule as one pure function: a voter counts once by their latest vote (a member key id for a group vote, the one-time key for a public one), a confirmation refreshes the time left to a full lifetime from the confirmation, capped at twice the type's default from creation (FR-6.3), and "probably gone" is FR-6.4 (a) or (b) exactly. `VoteRuleTest` covers every branch.
- The relay holds public votes with their reports and keeps a confirmed public report until its extended expiry, whichever arrives first; `GroupChannel` keeps the latest vote per member and extends a held group report the same way. A public report's vote also goes to each of this phone's groups, so members count it as a member's (threat model §5.7).
- `MeshNode.vote` allows one vote per report per phone and none on your own; every shown report carries its tally and this phone's vote.
- In the app: the report detail has Still there and Gone, which lock after the vote, the votes line split into the group's and nearby ("Crew: 2 confirmed · Nearby: 3 confirmed, 1 said gone", never "verified"), and a faded header with a callout naming which rule faded it. The pin fades too, and TalkBack adds the confirmations and "probably gone".
- Tests: `VoteMeshTest` on loopback, AC-7 (a confirmation reaches every phone and extends, capped at twice the default) and AC-8 (three dismissals fade the pin on every phone and it stays), and a group member's dismissal cleared by a later member's confirmation. `GroupBookTest` round-trips a group vote; `GroupFuzzTest` has an `inner-vote` seed.
- Deviation from the design: it said the other button "stays available" after voting, which would be a second vote; FR-6.1 allows one, so both lock. The design is aligned.

Still to do on this item: AC-7 and AC-8 on phones (AC-7's "within 10 s" needs the radio).

branch-merged-ok: the code merges with PR #6 while the phone checks stay open on this item.

## Review
