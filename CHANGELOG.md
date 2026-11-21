# Changelog

All notable changes are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [0.2.0] - 2026-11-21

### Added

- Groups, joined in person: one phone shows a join code that lasts two minutes and
  works once, the other scans it, and the two phones agree on keys over one direct Bluetooth hop.
  Every join is announced to the group.
- Group reports: encrypted, relayed by every phone, readable only by members, and shown as
  verified. You choose the audience when you send.
- Confirm or dismiss a report, with votes that spread through the mesh.
- Share your position with a group, opt-in and short-lived, also while Echo is off screen.
- Mute a group member on this phone only; filter the map to your groups; see members seen
  recently.
- Panic wipe in two actions, which destroys the storage key first.
- Discreet mode, with a Quick Settings tile.
- A warning on public steering reports ("go this way"), which may be a trap.
- A notice when the mesh around you is too thin to trust an empty map.
- Field diagnostics, counted in memory, with no positions or reports, shared only by hand.
- French and Spanish drafts of every screen (see the release notes: not reviewed yet).
- The settings screen.
- A transport guide for contributors, and a CI check that two builds of the release are identical.

### Changed

- "What Echo protects" lists the panic wipe, and every safety sentence cites the threat model.
- Accessibility: every action is named, lists read one row at a time, bottom sheets scroll so
  their buttons keep their size, and a stack of pins reads every report in it.

### Fixed

- Echo keeps running while the phone is locked and its storage key refuses to open.
- A refused location permission always offers Echo's page in Android's settings.
- Position sharing gets a location at once and keeps getting them while the phone is still.

## [0.1.0] - 2026-11-21

### Added

- Bluetooth LE transport: phones find each other and keep a link with rotating tokens, no stable
  identifier on the air, and a relay service that keeps working with the screen off.
- Signed public reports with a fixed wire format, fuzzed in CI, relayed by gossip with hop limits,
  per-peer budgets and expiry.
- Offline maps: import, keep, share and draw map files.
- Settings sealed with a Keystore key that can be destroyed.
- Screens: honest onboarding, the map with pins, dropping a report, the mesh status.
- The project skeleton: a multi-module Kotlin and Compose build, the `MeshTransport` contract with
  an in-memory transport for simulated phones, and build gates that refuse the `INTERNET`
  permission, proprietary or tracking dependencies, and imports across the transport and crypto
  boundaries.

### Fixed

- Two phones in range no longer lose each other for half an hour: either phone connects when the
  other can't hear it, a stopped scan or a refused advertisement is retried with backoff, and every
  unused link is closed.
