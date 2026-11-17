**Authoritative for:** which items exist and what state each is in, and the next free id.
**Not authoritative for:** why any item exists or how to do it — that is the item file.

# Backlog

The board. One row per live work item, grouped by status. Items live in
[`items/`](items/); finished work moves to [`archive/`](archive/).

<!-- NEXT-ID: WI-034 -->
<!-- Claim from this marker and bump it on your own branch. `node .ai/rungs.mjs check` refuses a duplicate. -->

## In progress

| Id | Title | Type | Branch |
| --- | --- | --- | --- |
| — | *nothing in flight* | | |

## Review

| Id | Title | Type | Branch |
| --- | --- | --- | --- |
| [WI-003](items/WI-003-wire-format-and-fuzzing.md) | Decide the wire format and fuzz its decoder in CI | feature | `claude/project-thread-67cn4e` |
| [WI-005](items/WI-005-loopback-crowd-simulator.md) | Grow the loopback adapter into a crowd simulator | feature | `claude/project-thread-67cn4e` |

## Planned

| Id | Title | Type |
| --- | --- | --- |
| [WI-001](items/WI-001-phase-0-foundations.md) | Phase 0 — Foundations: finish what every later phase builds on | epic |
| [WI-002](items/WI-002-accept-crypto-adr.md) | Review and accept the crypto design (ADR-0004) | docs |
| [WI-004](items/WI-004-map-renderer-and-download-adr.md) | Choose the offline map renderer and how map data arrives | spike |
| [WI-006](items/WI-006-phase-1-two-phones-one-report.md) | Phase 1 — Two phones, one report: ship 0.1.0 | epic |
| [WI-007](items/WI-007-ble-spike-reference-phones.md) | Prove BLE discovery and exchange on the three reference phones | spike |
| [WI-008](items/WI-008-public-report-signing.md) | Sign, encode and validate public reports | feature |
| [WI-009](items/WI-009-gossip-relay.md) | Relay reports by gossip with de-duplication, hop budget, rate limits and expiry | feature |
| [WI-010](items/WI-010-encrypted-local-store.md) | Encrypt everything the app stores, with a Keystore key | feature |
| [WI-011](items/WI-011-ble-transport-adapter.md) | Build the BLE transport adapter and the relay foreground service | feature |
| [WI-012](items/WI-012-offline-map.md) | Show an offline map: renderer wrapper, import, browser download and sharing | feature |
| [WI-013](items/WI-013-phase-1-screens.md) | Build the Phase 1 screens: onboarding draft, map with pins, drop a report, mesh status | feature |
| [WI-014](items/WI-014-transport-guide.md) | Write docs/transports.md: how to add a transport | docs |
| [WI-015](items/WI-015-release-0-1-0.md) | Set up release signing and cut 0.1.0 | chore |
| [WI-016](items/WI-016-phase-2-groups.md) | Phase 2 — Groups: ship 0.2.0 | epic |
| [WI-017](items/WI-017-group-keys-and-qr-join.md) | Create groups and join them in person by QR | feature |
| [WI-018](items/WI-018-encrypted-group-reports.md) | Send encrypted group reports that everyone relays and only members read | feature |
| [WI-019](items/WI-019-confirm-and-dismiss.md) | Confirm or dismiss a report, with votes that spread | feature |
| [WI-020](items/WI-020-position-sharing.md) | Share position with a group, opt-in and short-lived | feature |
| [WI-021](items/WI-021-mute-a-member.md) | Mute a group member on this phone | feature |
| [WI-022](items/WI-022-group-screens.md) | Build the group screens: create, join, roster, verified style and filters | feature |
| [WI-023](items/WI-023-phase-3-safety-and-street-use.md) | Phase 3 — Safety and street use: ship 0.3.0 | epic |
| [WI-024](items/WI-024-panic-wipe.md) | Panic wipe in under two seconds | feature |
| [WI-025](items/WI-025-discreet-mode.md) | Discreet mode | feature |
| [WI-026](items/WI-026-final-onboarding-copy.md) | Final onboarding and "What this app protects" copy, traced to the threat model | docs |
| [WI-027](items/WI-027-storage-and-air-audits.md) | Audit storage and radio for leaks | chore |
| [WI-028](items/WI-028-crowd-field-test.md) | Field-test in a crowd and tune lifetimes, rates and battery | spike |
| [WI-029](items/WI-029-phase-4-release-1-0.md) | Phase 4 — Release 1.0 | epic |
| [WI-030](items/WI-030-independent-security-review.md) | Independent security review and fixes | chore |
| [WI-031](items/WI-031-accessibility-pass.md) | Accessibility pass | chore |
| [WI-032](items/WI-032-reproducible-build.md) | Verify the build is reproducible and F-Droid ready | chore |
| [WI-033](items/WI-033-release-1-0.md) | Final docs and cut 1.0.0 | chore |

## Accepted

| Id | Title | Type |
| --- | --- | --- |
| — | | |

## Proposed

| Id | Title | Type |
| --- | --- | --- |
| — | | |

## Deferred

| Id | Title | Revisit when |
| --- | --- | --- |
| — | | |

---

Rejected and completed items keep their files — see [`archive/`](archive/). A rejection is part of
the record, with its reason; deleting one loses the answer to "why not?" and invites the same
proposal again.
