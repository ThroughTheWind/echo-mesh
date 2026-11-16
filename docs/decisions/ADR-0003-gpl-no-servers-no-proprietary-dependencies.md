---
id: ADR-0003
title: License the app GPL-3.0, run no server, ask for no internet, and ship no proprietary or tracking dependency
status: accepted
date: 2026-11-16
supersedes:
superseded_by:
---

# ADR-0003 — License the app GPL-3.0, run no server, ask for no internet, and ship no proprietary or tracking dependency

## Context

The app exists because networks are cut where people gather, and the people who use it may be
targeted. A server would be a single point to block, subpoena or seize, and a list of who was there.
Any network call beyond the radio link can reveal that a phone runs the app. A proprietary or tracking
library is code nobody can audit, running next to the most sensitive data the phone holds; the best
known protest mesh app was shown to leak and be impersonable. Google Play Services, the usual way to
do nearby messaging, location and maps on Android, is both proprietary and a channel to Google.
Trust depends on anyone being able to read and rebuild what they install, and F-Droid, the natural
store for it, refuses proprietary libraries.

## Decision

- The app is licensed **GPL-3.0-only**.
- **No servers.** The app talks only to other phones running it, over short-range peer-to-peer radio
  (ADR-0002). It has no backend, no account, no relay over the internet, and never phones home.
- **No `INTERNET` permission by default.** The one planned exception is an explicit, user-started
  download of offline map data, which can also be sideloaded from a file. How that download is
  allowed (a separate build, a separate app, or a permission confined to one module) is its own ADR.
  Until an ADR allows it, `checkManifestPermissions` fails any variant whose merged manifest asks for
  `INTERNET`, including one added by a library. Android needs `INTERNET` to open any socket, so a
  transport that moves data over Wi-Fi Direct or Wi-Fi Aware sockets also needs that ADR first.
- **No proprietary or tracking dependency**: no Google Play Services (so no Nearby Connections, Fused
  Location or Google Maps), no Firebase, no Play libraries, no closed SDK, and no ads, analytics,
  telemetry or crash reporting of any kind, open-source ones included. Every dependency has a
  GPL-3.0-compatible license. `gradle/forbidden-dependencies.txt` lists forbidden groups;
  `checkForbiddenDependencies` fails if any module's resolved runtime classpath contains one, and
  the fast `privacy` gate checks the version catalog without Gradle. Groups are listed narrowly, so
  open libraries that share a vendor prefix (for example `com.google.crypto.tink`) stay allowed.
- Releases are signed APKs on GitHub Releases, built reproducibly enough for F-Droid. No Play Store
  at first.

## Consequences

**Good**

- Nothing to block, seize or subpoena but the phones themselves, and nothing on the internet learns
  who uses the app.
- Anyone who ships a modified version must publish its source, which blocks closed, tracker-filled
  clones of a tool people trust with their safety.
- The gates make the property mechanical: a dependency or a permission that breaks it fails the
  build instead of waiting for a reviewer to notice.
- Eligible for F-Droid.

**Costs and risks**

- No crash reports at all: bugs reach us only through issues and testers. Testing on the reference
  phones has to make up for it.
- No Play Services means no Nearby Connections: the radio code is ours to write and to keep working
  across phone models.
- The map download needs a careful design so the exception does not become a general internet
  permission.
- The forbidden list can miss a tracker under a new group. Exodus Privacy's tracker list is the
  reference for extending it, and review still reads new dependencies.

## Alternatives considered

**Apache-2.0 or MIT** lost because they let anyone publish a closed, tracker-filled fork.

**An optional internet relay** (as Bridgefy and FireChat used) lost because it reintroduces a server,
a metadata trail and a dependency on the network that is cut when the app is needed.

**Google Nearby Connections** lost despite solving discovery and transport across radios, because
it is proprietary Play Services code and requires Play Services on the phone.

**Open-source crash reporting** (for example ACRA to a self-hosted endpoint) lost because a crash
report sent over the internet tells an observer the phone runs the app, and its content can carry
positions or keys.

## Revisit triggers

1. The map-download ADR is written: it supersedes this record's `INTERNET` clause for that one path only.
2. A transport needs sockets: its ADR must say how traffic is kept to the peer-to-peer interface.
3. A library this app needs is published only under a GPL-3.0-incompatible license.

## Admission check

Against [the rule](README.md): (1) constrains future work ✅ · (2) a real alternative was rejected
with a stated reason ✅ · (3) reversing later costs meaningfully more ✅ · (4) not owned by another
document ✅ · (5) not an implementation detail ✅.
