---
id: ADR-0002
title: Reach other phones through radio transport adapters behind a capability contract, one module per radio
status: accepted
date: 2026-11-16
supersedes:
superseded_by:
---

# ADR-0002 — Reach other phones through radio transport adapters behind a capability contract, one module per radio

## Context

Phones at a protest can reach each other over several short-range radios, and none is best
everywhere. Bluetooth LE reaches every phone but carries tiny frames; Wi-Fi Direct is fast but needs
a group to form and an IP socket; Wi-Fi Aware discovers without an access point but is missing on
many phones. Each behaves differently in the background, under Doze and per Android version, and
each will need workarounds for specific phones. If the mesh or the UI knew which radio it used, every
new radio and every quirk would touch the relay logic, which is the code that must stay small and
reviewed. This is the same problem Zapper solved for TV brands (Zapper's ADR-0002).

## Decision

- Core and UI talk to radios only through the `MeshTransport` contract in `:core:mesh`. No code
  outside an adapter module knows whether it is talking over BLE, Wi-Fi Direct or Wi-Fi Aware.
- A transport declares what it **can do** as `TransportCapabilities`: range class, bandwidth class,
  largest frame, whether it can broadcast, whether it works in the background, and whether it needs
  location permission. Core picks transports and the UI explains state from capabilities only.
- A transport moves **opaque frames** to peers it knows only by a per-session `PeerHandle`. It never
  parses, signs, encrypts, relays or de-duplicates; that is `:core:mesh` and `:core:crypto`. A
  transport is untrusted plumbing, and a frame from it is hostile until core verifies it.
- Each radio lives in its own module, `:adapters:<radio>`, depending on `:core:mesh` (and
  `:core:model` if needed) only. Adapters never depend on each other, on `:app`, `:core:data` or
  `:core:crypto`. The app names adapters only in `TransportRegistration.kt`.
- `:adapters:loopback` is an in-memory transport that simulates many phones on the JVM. Every
  adapter, real or simulated, passes the shared `MeshTransportContract` tests. Release builds do not
  link loopback.
- `./gradlew checkTransportBoundaries` fails the build if `app/` or `core/` imports an adapter outside
  the registration files, or an adapter imports another adapter.

## Consequences

**Good**

- Adding a radio is a new module plus one registration line, with no change to the mesh or the UI.
- The relay, de-duplication and crypto logic is written once, tested on loopback with simulated
  crowds and hostile peers, and reviewed in one place.
- A radio that misbehaves on one phone model is fixed inside its adapter.

**Costs and risks**

- The contract is shaped before the first real radio exists. Expect it to change when BLE lands,
  and again with the second radio.
- Treating transports as dumb pipes gives up radio-specific tricks such as putting a report directly
  in a BLE advertisement's service data. If one is worth it, it needs a capability, not a bypass.
- Capability classes (`ROOM`, `STREET`, `BLOCK`) are coarse by design; transport research must give
  each adapter honest values.

## Alternatives considered

**Building on Google Nearby Connections**, which already hides the radios behind one API, lost
because it is part of Play Services (ADR-0003).

**One relay implementation per radio** lost because the security-critical code would exist several
times, and frames could not hop from a BLE phone to a Wi-Fi Direct phone.

**Building on an existing open mesh library** (for example Briar's Bramble transport layer) lost as
the architecture, because none exposes a capability contract for a map app's tiny, expiring reports;
transport research may still reuse parts of one inside an adapter if its license is GPL-3.0-compatible.

## Revisit triggers

1. A radio cannot be expressed as opaque frames plus capabilities without bending the contract.
2. Two radios need to cooperate on one link (for example BLE discovery handing over to Wi-Fi Direct)
   in a way the contract cannot describe.

## Admission check

Against [the rule](README.md): (1) constrains future work ✅ · (2) a real alternative was rejected
with a stated reason ✅ · (3) reversing later costs meaningfully more ✅ · (4) not owned by another
document ✅ · (5) not an implementation detail ✅.
