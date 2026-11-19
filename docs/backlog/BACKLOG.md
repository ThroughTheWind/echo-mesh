**Authoritative for:** which items exist and what state each is in, and the next free id.
**Not authoritative for:** why any item exists or how to do it — that is the item file.

# Backlog

The board. One row per live work item, grouped by status. Items live in
[`items/`](items/); finished work moves to [`archive/`](archive/).

<!-- NEXT-ID: WI-074 -->
<!-- Claim from this marker and bump it on your own branch. `node .ai/rungs.mjs check` refuses a duplicate. -->

## In progress

| Id | Title | Type | Branch |
| --- | --- | --- | --- |
| [WI-007](items/WI-007-ble-spike-reference-phones.md) | Prove BLE discovery and exchange on the three reference phones | spike | `claude/project-thread-67cn4e` |
| [WI-017](items/WI-017-group-keys-and-qr-join.md) | Create groups and join them in person by QR | feature | `claude/project-thread-67cn4e` |
| [WI-018](items/WI-018-encrypted-group-reports.md) | Send encrypted group reports that everyone relays and only members read | feature | `claude/project-thread-67cn4e` |

## Review

| Id | Title | Type | Branch |
| --- | --- | --- | --- |
| [WI-010](items/WI-010-encrypted-local-store.md) | Encrypt everything the app stores, with a Keystore key | feature | `claude/project-thread-67cn4e` |
| [WI-011](items/WI-011-ble-transport-adapter.md) | Build the BLE transport adapter and the relay foreground service | feature | `claude/project-thread-67cn4e` |
| [WI-012](items/WI-012-offline-map.md) | Show an offline map: renderer wrapper, import, browser download and sharing | feature | `claude/project-thread-67cn4e` |
| [WI-013](items/WI-013-phase-1-screens.md) | Build the Phase 1 screens: onboarding draft, map with pins, drop a report, mesh status | feature | `claude/project-thread-67cn4e` |
| [WI-014](items/WI-014-transport-guide.md) | Write docs/transports.md: how to add a transport | docs | `claude/project-thread-67cn4e` |

## Planned

| Id | Title | Type |
| --- | --- | --- |
| [WI-006](items/WI-006-phase-1-two-phones-one-report.md) | Phase 1 — Two phones, one report: ship 0.1.0 | epic |
| [WI-015](items/WI-015-release-0-1-0.md) | Set up release signing and cut 0.1.0 | chore |
| [WI-016](items/WI-016-phase-2-groups.md) | Phase 2 — Groups: ship 0.2.0 | epic |
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
| [WI-034](items/WI-034-steering-report-warnings.md) | Warn on public steering reports and hide their public counts | feature |
| [WI-035](items/WI-035-low-density-honesty.md) | Tell people when the mesh around them is too thin | feature |
| [WI-036](items/WI-036-field-diagnostics.md) | Export a field diagnostics file with no positions or reports | feature |
| [WI-037](items/WI-037-languages-en-fr-es.md) | Ship Echo in English, French and Spanish | feature |
| [WI-038](items/WI-038-release-key-custody.md) | Publish who signs Echo and how to check it | docs |
| [WI-039](items/WI-039-phase-5-release-1-1.md) | Phase 5 — Release 1.1: groups through the whole day | epic |
| [WI-040](items/WI-040-accept-threat-model-1-1.md) | Review and accept the threat model's 1.1 section | docs |
| [WI-041](items/WI-041-canned-group-phrases.md) | Send canned phrases inside a group | feature |
| [WI-042](items/WI-042-group-rally-point.md) | Keep one rally point per group | feature |
| [WI-043](items/WI-043-nearby-danger-alerts.md) | Vibrate on danger reported nearby | feature |
| [WI-044](items/WI-044-group-check-ins.md) | Show who missed a check-in | feature |
| [WI-045](items/WI-045-jail-support-card.md) | Keep a jail-support card on the phone | feature |
| [WI-046](items/WI-046-app-lock.md) | Lock the app with a PIN and an attempt limit | feature |
| [WI-047](items/WI-047-duress-pin-and-auto-wipe.md) | Add a duress PIN and auto-wipe | feature |
| [WI-048](items/WI-048-encrypted-report-cache.md) | Cache live reports encrypted so a restart keeps the map | feature |
| [WI-049](items/WI-049-adaptive-scanning.md) | Scan less when the crowd is dense | feature |
| [WI-050](items/WI-050-briar-handoff.md) | Hand off to Briar for chat | feature |
| [WI-051](items/WI-051-release-1-1.md) | Cut 1.1.0 | chore |
| [WI-052](items/WI-052-phase-6-release-2-0.md) | Phase 6 — Release 2.0: organisations, packs and a second radio | epic |
| [WI-053](items/WI-053-accept-organisation-design.md) | Accept the organisation credentials design and the threat model's 2.0 section | docs |
| [WI-054](items/WI-054-outside-review-2-0.md) | Outside review of the organisation layer and Wi-Fi Aware identifiers | chore |
| [WI-055](items/WI-055-pack-and-catalog-formats.md) | Decide the event pack and catalog formats | docs |
| [WI-056](items/WI-056-trust-store.md) | Let users choose which organisations they trust | feature |
| [WI-057](items/WI-057-role-issuance.md) | Issue role credentials in person | feature |
| [WI-058](items/WI-058-role-reports-and-stations.md) | Show role badges, stations and role confirmations | feature |
| [WI-059](items/WI-059-organiser-announcements.md) | Let organisers make rare, signed announcements | feature |
| [WI-060](items/WI-060-signed-catalogs-and-crisis.md) | Ship signed catalogs, including Crisis, and crisis groups | feature |
| [WI-061](items/WI-061-group-phrases-in-catalog.md) | Move phrases and announcements into the catalog | chore |
| [WI-062](items/WI-062-event-packs.md) | Import and show event packs | feature |
| [WI-063](items/WI-063-wifi-aware-spike.md) | Prove Wi-Fi Aware on the reference phones and settle the INTERNET question | spike |
| [WI-064](items/WI-064-wifi-aware-adapter.md) | Build the Wi-Fi Aware transport adapter | feature |
| [WI-065](items/WI-065-share-echo-offline.md) | Share Echo itself offline with an authenticity check | feature |
| [WI-066](items/WI-066-bulk-share-maps-and-packs.md) | Share maps and event packs over Wi-Fi Aware | feature |
| [WI-067](items/WI-067-release-2-0.md) | Cut 2.0.0 | chore |
| [WI-068](items/WI-068-phase-7-later-evidence-gated.md) | Phase 7 — Later: evidence-gated extensions | epic |

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
| [WI-069](items/WI-069-circles.md) | Circles: long-lived groups that spawn event groups | Field use or user reports show the 7-day group limit pushing groups to other tools. |
| [WI-070](items/WI-070-group-management-at-scale.md) | Remove members, chain QR joins and link groups | Groups of 20 or more are common in field use, or a compromised member can't be handled by re-forming the group. |
| [WI-071](items/WI-071-areas-to-avoid.md) | Shade areas to avoid from trusted reports | Field tests show pins become unreadable in dense areas, after roles (WI-058) ship. |
| [WI-072](items/WI-072-lora-bridge.md) | Bridge distant crowds with LoRa / Meshtastic hardware | Marshal or medic teams ask for it with hardware in hand, after 2.0. |
| [WI-073](items/WI-073-legacy-build.md) | Offer a legacy build for Android 8 to 11 | Field diagnostics or user reports show a significant share of intended users on Android 8–11. |

---

Rejected and completed items keep their files — see [`archive/`](archive/). A rejection is part of
the record, with its reason; deleting one loses the answer to "why not?" and invites the same
proposal again.
