**Authoritative for:** the order in which the MVP is delivered, and what each phase must prove before the next starts.
**Not authoritative for:** what the app does (the [spec](SPEC.md)), how it looks (the [design system](design/README.md)), what it defends against (the [threat model](threat-model.md)), or the state of any item (the [backlog](backlog/BACKLOG.md)).

# Roadmap

The MVP from the [spec](SPEC.md), cut into phases. **Basics first:** two reference phones reliably
exchanging a signed, expiring report over one transport and showing it on an offline map ships
before groups, a second transport or any extras. Each phase is an epic work item in the [backlog](backlog/BACKLOG.md); its children are the deliverables.

| Phase | Goal | Exit (spec acceptance criteria) | Epic |
| --- | --- | --- | --- |
| 0. Foundations | Repo, stack, CI and gates, transport contract, loopback mesh, threat model v1, wire format | AC-20 → AC-23 | WI-001 |
| **1. Two phones, one report** | A signed, expiring public report crosses from one reference phone to the other over one transport and shows on an offline map. Ships as **0.1.0** | AC-1 → AC-6a, AC-9 → AC-12 | WI-006 |
| 2. Groups | QR join, encrypted group reports, confirm/dismiss, position sharing, mute a member. Ships as **0.2.0** | AC-7, AC-8, AC-13 → AC-16, AC-24, AC-25 | WI-016 |
| 3. Safety and street use | Panic wipe, discreet mode, final onboarding copy, a crowd field test. Ships as **0.3.0** | AC-17 → AC-19 | WI-023 |
| 4. Release 1.0 | Independent security review and fixes, accessibility, docs, reproducible signed release, and the extended spec's 1.0 additions (F15–F19). Ships as **1.0.0** | All ACs, AC-26 → AC-31 | WI-029 |
| 5. Release 1.1 | Groups through the whole day: phrases, rally point, danger alerts, check-ins and jail-support card, app lock and duress PIN, report cache, adaptive scanning, Briar handoff (F20–F27). Ships as **1.1.0** | AC-32 → AC-39 | WI-039 |
| 6. Release 2.0 | Organisations, roles and announcements, event packs, signed catalogs including crisis, Wi-Fi Aware, sharing Echo offline (F28–F34), after an outside review. Ships as **2.0.0** | AC-40 → AC-46 | WI-052 |
| 7. Later | Circles, group management at scale, areas to avoid, LoRa bridge, legacy build (F35–F39), each started when its evidence condition is met | Written when each starts | WI-068 |

## Phase 0 — Foundations

Nothing user-visible, but everything after depends on it.

1. Scaffold the build, rungs, CI, `checkTransportBoundaries`, the dependency gate and the manifest gate (stack thread).
2. ADR-0001 stack, ADR-0002 transports behind a capability contract, ADR-0003 GPL, no proprietary dependencies, no servers, no internet by default.
3. Threat model v1 and the crypto ADR (threat model thread).
4. `MeshTransport` contract in `:core:mesh` and `:adapters:loopback`.
5. Wire format ADR and the decoder fuzz target: the format is versioned from day one.

## Phase 1 — Two phones, one report

Order matters: the spike settles the radio facts the rest depends on.

1. **BLE spike on the reference phones**: prove BLE can discover a peer and move one message both ways, screen off, with the permissions planned, on the Pixel 11 Pro, Pixel 9a and OPPO Find X3 Pro. The OPPO tests whether ColorOS's battery manager kills the relay service. Record each phone's Android version and extended-advertising support. Needs Antoine and the phones.
2. Report model, signing and validation in `:core:model` and `:core:crypto`, tested on loopback including hostile peers.
3. Relay with hop limit, de-duplication, per-peer rate limits and expiry in `:core:mesh`.
4. The first transport adapter.
5. Encrypted local store in `:core:data`.
6. Offline map: renderer wrapper in `:core:map`, import a map file, browser download path.
7. Screens: honest onboarding (first draft), map with pins, drop a report, mesh status.
8. Signed release pipeline, so 0.1.0 can go to the two phones.

Steps 2, 3 and 5 can run in parallel once Phase 0 is done; step 4 needs the spike.

## Phase 2 — Groups

Group keys and QR join, encrypted group reports and the verified style, confirm and dismiss, opt-in
position sharing. Groups need the threat model's group protocol settled before any code.

## Phase 3 — Safety and street use

Panic wipe, discreet mode, final onboarding copy traced to the threat model, and a field test in a
real crowd (or the closest safe equivalent) to tune lifetimes, rates and battery.

## Phase 4 — Release 1.0

Independent security review of the protocol, crypto use and wire decoder, and fixing what it
finds; accessibility pass; reproducible build checked; `SECURITY.md` and transport guide final.
Plus the 1.0 additions from the [extended spec](SPEC-EXTENDED.md): steering-report warnings (F15),
low-density honesty (F16), field diagnostics (F17, used in Phase 3's field test), English, French
and Spanish (F18), release key custody (F19), and re-tuning thresholds from field data.

## Phase 5 — Release 1.1

What affinity groups need before and after the march, with no new trust model: everything reuses
the group frame or stays on the phone. WI-040 (accept the threat model's 1.1 section) comes first,
because phrases, the duress PIN and the report cache change what a seized phone or a member can
learn. WI-041 (canned phrases) comes before WI-042 (rally point) and WI-044 (check-ins), which reuse
it. The rest are independent, and WI-051 cuts 1.1.0.

## Phase 6 — Release 2.0

Organisations without a server, plans everyone carries, and a second radio for bulk transfer.

1. WI-053 accepts the organisation credentials design and the threat model's 2.0 section, and WI-055 fixes the pack and catalog formats.
2. WI-056 (trust store), then WI-057 (issuing roles), WI-058 (badges and stations) and WI-059 (announcements).
3. WI-060 (signed catalogs, including Crisis and crisis groups), then WI-061 (phrases into the catalog) and WI-062 (event packs).
4. WI-063 proves Wi-Fi Aware on the reference phones and settles how a data path meets ADR-0003's `INTERNET` rule, then WI-064 (adapter), WI-065 (share Echo offline) and WI-066 (share maps and packs).
5. WI-054, the outside review, gates WI-067, which cuts 2.0.0.

## Phase 7 — Later

Deferred items, each with its own revisit trigger in the [backlog](backlog/BACKLOG.md): WI-069
circles, WI-070 group management at scale, WI-071 areas to avoid, WI-072 LoRa bridge and WI-073
legacy build.

F-Droid submission follows the first reproducible release. Chat, photos and internet relays are not
planned (spec §3.2, extended spec §3.1).
