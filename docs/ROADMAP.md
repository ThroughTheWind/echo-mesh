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
| 4. Release 1.0 | Independent security review and fixes, accessibility, docs, reproducible signed release. Ships as **1.0.0** | All ACs | WI-029 |

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

## After 1.0

A second transport (Wi-Fi Aware, per the research; Wi-Fi Direct is dropped), app lock / duress PIN, removing
a group member by re-keying, F-Droid. Chat, photos and internet relays are not planned (spec §3.2).
