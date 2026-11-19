# Echo

A free, open-source Android app that works like Waze for people at a protest. Phones nearby share
short-lived, map-pinned reports with each other ("police line here", "medic", "exit open") over
Bluetooth LE and Wi-Fi peer-to-peer. It needs **no server and no internet**: reports hop from phone
to phone and expire.

**Status: pre-alpha. Nothing here is ready to rely on at a protest.** The app has not had the
independent security review it needs before 1.0 ([SECURITY.md](SECURITY.md)).

## What it will and will not do

- It talks only to other phones running it, over short-range radio. No accounts, no servers, no
  analytics, no crash reports, and no `INTERNET` permission ([ADR-0003](docs/decisions/ADR-0003-gpl-no-servers-no-proprietary-dependencies.md)).
- No Google Play Services and no proprietary code. Maps are OpenStreetMap data rendered offline.
- What it protects and what it does not will be written down plainly in the threat model before any
  release, and the app will say the same thing in its own words.

## Building

You need a JDK 21 and the Android SDK (Android Studio installs both).

```bash
./gradlew :app:assembleDebug      # debug APK in app/build/outputs/apk/debug/
./gradlew test                    # unit tests, including simulated phones on the JVM
./gradlew -p build-logic test     # tests of the build's own privacy gates
node .ai/rungs.mjs check          # documentation and bookkeeping gates (Node 22)
```

Everything CI runs is in [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

## How it is built

Kotlin and Jetpack Compose, one Gradle module per concern ([ADR-0001](docs/decisions/ADR-0001-kotlin-compose-multi-module.md)).
Each radio is an adapter behind the `MeshTransport` capability contract, so the mesh, the crypto and
the UI never know which radio they use ([ADR-0002](docs/decisions/ADR-0002-transport-adapters.md)).
It is a sibling of [Zapper](https://github.com/ThroughTheWind/zapper) and shares its methodology.

## Checking that your copy is ours

Every release is signed with one key. Android shows nothing about it, so check it yourself: the
signing certificate's SHA-256 fingerprint below must match the one on the release page. A copy with another fingerprint is not ours, whoever gave it to you.

<!-- signing-fingerprint -->
No signed release yet. The first one publishes its fingerprint here.
<!-- /signing-fingerprint -->

## Contributing and security

See [CONTRIBUTING.md](CONTRIBUTING.md). **Report vulnerabilities privately**, never in a public
issue: [SECURITY.md](SECURITY.md).

## License

[GPL-3.0-only](LICENSE).
