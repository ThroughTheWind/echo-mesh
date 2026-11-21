# Changelog

All notable changes are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

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
