---
description: >-
  Rules for radio transport adapters and the mesh contract. Load when editing anything under
  adapters/ or core/mesh/.
paths:
  - "adapters/**"
  - "core/mesh/**"
enforcement: gated
---

# Transports and the mesh

- Core and UI know a transport only as `MeshTransport` and its `TransportCapabilities`, never as
  BLE, Wi-Fi Direct or Wi-Fi Aware (ADR-0002). `./gradlew checkTransportBoundaries` fails if `app/`
  or `core/` imports an adapter outside `TransportRegistration.kt`, or if one adapter imports another.
- An adapter depends on `:core:mesh` (and `:core:model` if it must) only. Never on `:app`,
  `:core:data`, `:core:crypto` or another adapter. Gradle enforces the graph; review enforces the list.
- A transport moves opaque frames. It does not parse, sign, encrypt, relay or de-duplicate them:
  that is `:core:mesh` and `:core:crypto`. A frame from the radio is hostile until core verifies it.
- Every adapter's tests extend `MeshTransportContract` (`core/mesh/src/testFixtures`). Mesh logic is
  tested on `:adapters:loopback`, including peers that send malformed, oversized and flooding traffic.
- No stable identifier on the air: a `PeerHandle` lives for one session, is never stored and never
  sent. Rely on the OS's MAC randomisation; never read, log or work around it.
- Declare capabilities honestly. If a radio cannot work in the background or needs a permission on
  some Android version, the capability says so, so the UI can tell the user.
- No transport opens a socket until an ADR allows the `INTERNET` permission it needs (ADR-0003);
  `checkManifestPermissions` fails the build otherwise.
