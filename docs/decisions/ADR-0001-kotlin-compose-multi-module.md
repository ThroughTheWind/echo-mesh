---
id: ADR-0001
title: Build the app natively in Kotlin and Jetpack Compose, as a multi-module Gradle project with convention plugins
status: accepted
date: 2026-11-16
supersedes:
superseded_by:
---

# ADR-0001 — Build the app natively in Kotlin and Jetpack Compose, as a multi-module Gradle project with convention plugins

## Context

The app is a free, open-source, serverless map for people at a protest, on Android phones only. It
drives radios directly (Bluetooth LE advertising and GATT, Wi-Fi Direct, Wi-Fi Aware), keeps running
under Doze and the foreground-service rules, renders an offline map, and does cryptography on every
frame it relays. The protocol, crypto and relay logic is where the risk is, and it must be testable
as many simulated phones, including hostile ones, without a radio or an emulator. It is a sibling of
Zapper, which settled the same stack question for a remote app, and sharing that stack lets both
projects share methodology, tooling and reviewers. There is no iOS, web or desktop target.

## Decision

- The app is **Kotlin**, with **Jetpack Compose and Material 3** for all UI. No XML views.
- The build is a **multi-module Gradle project** with Kotlin DSL and one version catalog
  (`gradle/libs.versions.toml`) for every version and SDK level:
  - `:app` — the Android application: screens, navigation, wiring, transport registration.
  - `:core:model` — pure Kotlin: reports, pins, groups, time-to-live.
  - `:core:mesh` — pure Kotlin: the `MeshTransport` capability contract, peer handling, gossip,
    relay and de-duplication.
  - `:core:crypto` — pure Kotlin: identities, group keys, signing and encryption, wrapping the
    library the crypto ADR picks. The only module allowed to import a cryptographic API.
  - `:core:data` — Android library: the encrypted local store and settings.
  - `:core:map` — Android library: offline map loading and rendering.
  - `:core:designsystem` — Android library: the Compose theme and shared components.
  - `:adapters:<radio>` — one Android library per radio (`ble`, `wifidirect`, `wifiaware`), plus
    `:adapters:loopback`, a pure-Kotlin in-memory transport for simulated phones (ADR-0002).
- Modules that do not need Android are **plain Kotlin/JVM**, so mesh, protocol and crypto code is
  unit-tested on the JVM against simulated peers.
- Shared build configuration lives in **convention plugins** in `build-logic/`
  (`conventions.jvm.library`, `conventions.android.library`, `conventions.android.application`,
  `conventions.gates`). Zapper deferred these until it had more than three Android modules; this
  project starts with seven, and the plugins are also where the privacy gates are wired into every
  module, so a new module cannot forget them.
- Async work is **Kotlin coroutines and Flow**. Local data is **kotlinx.serialization** and
  **Jetpack DataStore**. The over-the-air format is not decided here: radio frames are tiny and
  signed over exact bytes, so the wire format gets its own ADR.
- Dependency injection is **manual constructor injection** through one app-level container.
- Minimum SDK is **31** (Android 12), Antoine's decision on 2026-11-16, replacing the brief's 26.
  From 31, Bluetooth scanning and advertising have their own runtime permissions and need no
  location permission, and the Keystore offers unlocked-device-required keys for the encrypted
  store on every supported phone.
- No OkHttp or other HTTP client unless the map-download ADR needs one, and then only in the module
  that ADR names.

## Consequences

**Good**

- Direct access to every radio API Android exposes without Play Services: `BluetoothLeAdvertiser`,
  `BluetoothLeScanner`, GATT, `WifiP2pManager`, `WifiAwareManager`, foreground services.
- The mesh, crypto and relay code runs on the JVM against `:adapters:loopback`: a crowd of simulated
  phones, some hostile, in milliseconds. That is where most bugs that matter will be found.
- Module boundaries make ADR-0002 structural: an adapter that imports `:app` does not compile, and
  the import gates catch what the module graph cannot.
- The privacy gates (ADR-0003) are applied by the convention plugins, so they cover every module.

**Costs and risks**

- Convention plugins are code to maintain, and an error in them breaks every module at once. Their
  gate logic is plain Kotlin with its own tests (`./gradlew -p build-logic test`).
- No DI framework means wiring by hand. Fine at this size; noise past a few dozen bindings.
- minSdk 31 excludes phones on Android 11 and older, which are more common among people who cannot
  afford a recent phone. Accepted for a simpler permission model and a stronger encrypted store.
- Contributors need Android Studio and a JDK; CI needs the Android SDK.

## Alternatives considered

**Flutter** and **React Native** lost because their only advantage is iOS, which is out of scope,
while every radio API would sit behind platform channels written in Kotlin anyway, and a JS or Dart
layer adds another place for a parser bug to live.

**Kotlin Multiplatform with Compose Multiplatform** lost for now: it adds build complexity for a
second platform nobody asked for. The pure-Kotlin core keeps that door open.

**Module build files without convention plugins**, as Zapper started, lost because seven Android
modules would repeat the same setup and each would have to remember the privacy gates.

**minSdk 26**, as in the brief and Zapper, lost (Antoine, 2026-11-16): on 26 to 30, BLE scanning
needs location permission and location services switched on, which is both a worse ask of people
at a protest and a privacy cost, and older Keystores cannot require an unlocked device.

**Hilt** and **Koin** lost against manual DI for the same reasons as in Zapper: build time or
runtime failures to wire a few objects.

## Revisit triggers

1. The project takes on iOS or another platform: reopen Kotlin Multiplatform.
2. The manual container passes about thirty bindings, or wiring bugs appear: adopt Hilt.
3. Protest-support groups report that many of the people they serve use phones older than Android 12.

## Admission check

Against [the rule](README.md): (1) constrains future work ✅ · (2) a real alternative was rejected
with a stated reason ✅ · (3) reversing later costs meaningfully more ✅ · (4) not owned by another
document ✅ · (5) not an implementation detail ✅.
