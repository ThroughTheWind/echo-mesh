**Authoritative for:** how to add a radio transport to Echo, what the gates check about one, and the radio facts measured on the reference phones.
**Not authoritative for:** why transports are adapters (ADR-0002), what the mesh does with frames (`core/mesh`), or the comparison of radios (`docs/research/peer-to-peer-transports.md`).

# Adding a transport

Echo reaches other phones through **transports**: one Gradle module per radio under `adapters/`,
each implementing `MeshTransport` (`core/mesh/src/main/kotlin/.../core/mesh/MeshTransport.kt`).
Core and UI know a transport only through that interface and its `TransportCapabilities`, never by
name ([ADR-0002](decisions/ADR-0002-transport-adapters.md)). The Bluetooth LE adapter,
[`adapters/ble`](../adapters/ble/README.md), is the worked example throughout. The short rules are in
[`.ai/rules/transports.md`](../.ai/rules/transports.md).

## 1. What a transport is, and is not

A transport is untrusted plumbing. It finds phones nearby, opens links to them, and moves **opaque
frames** both ways. It never parses, signs, encrypts, relays or de-duplicates a frame: core does all
of that, and treats every frame from a transport as hostile until it has verified it.

| A transport does | A transport never does |
| --- | --- |
| Discover nearby phones and be discoverable | Read or log a frame's content |
| Emit `PeerSeen`, `PeerLost`, `FrameReceived` | Decide what to relay |
| Refuse a frame above `maxFrameBytes` with `FrameTooLargeException` | Keep any identity across sessions |
| Say why it cannot start (`TransportUnavailableException`) | Read, log or work around the OS's MAC randomisation |
| Drop a misbehaving link, and only that link | Open an internet socket (ADR-0003) |

## 2. The contract

`MeshTransport` has six members:

- `capabilities`: what it can do, declared honestly (§4).
- `events`: one flow with one collector, the mesh. Each event is delivered once.
- `start()`: discover and be discoverable. Throws `TransportUnavailableException(reason)` when it
  cannot run now: `RADIO_OFF`, `PERMISSION_MISSING` or `UNSUPPORTED`. The mesh status turns the
  reason into a fix the user can tap (spec FR-3.5).
- `stop()`: stops every radio use (FR-3.1). Peer handles from before are void.
- `send(peer, frame)` and `broadcast(frame)`: a peer that went out of range is not an error; the
  frame is dropped and a `PeerLost` follows.

A `PeerHandle` is random, per link or per session, never derived from an address, never stored and
never sent. The BLE adapter makes a new one from 8 random bytes for every link.

## 3. Nothing stable on the air

People who use Echo may be targeted, so a transport must put **no stable identifier on the air**
(threat model §5.1, spec FR-3.4). For BLE that means:

- The advertised token and the L2CAP PSM are random and change together at least every 10 minutes,
  with jitter so phones don't rotate in step. Every link drops at rotation, so phones that met before
  reconnect as strangers.
- The device name is never advertised. `AdvertisementPrivacyTest` reads `AndroidBleRadio.kt` and
  fails if any advertisement builder may include it, or anything else that names the phone.
- No pairing, no bonding, no GATT server.

A new transport needs the same: a test that proves what it puts on the air carries nothing fixed.

## 4. Capabilities

Declare what the radio really does on the phones Echo supports (Android 12, API 31, and later).
The UI uses these to tell the user what to expect; an optimistic value is a lie the user acts on.

| Field | BLE | Meaning |
| --- | --- | --- |
| `range` | `STREET` | `ROOM`, `STREET` or `BLOCK` |
| `bandwidth` | `LOW` | `TINY`, `LOW` or `HIGH` |
| `maxFrameBytes` | 256 | The largest frame accepted. The largest frame core sends is 240 bytes (ADR-0005) |
| `canBroadcast` | true | `broadcast` reaches every peer in range |
| `worksInBackground` | true | Keeps working with the screen off, under the relay service |
| `needsLocationPermission` | false | BLE on API 31+ uses `neverForLocation` |

## 5. Step by step

1. **Create the module.** `adapters/<radio>/build.gradle.kts` applies `conventions.android.library`
   (or `conventions.jvm.library` if no Android API is needed) and depends on `:core:mesh` only:

   ```kotlin
   plugins {
       alias(libs.plugins.conventions.android.library)
   }

   dependencies {
       api(project(":core:mesh"))
       testImplementation(testFixtures(project(":core:mesh")))
   }
   ```

   Add `include(":adapters:<radio>")` to `settings.gradle.kts`. Never depend on `:app`,
   `:core:data`, `:core:crypto` or another adapter.

2. **Split logic from the radio.** Put the protocol (sessions, rotation, framing, limits) in a class
   that takes a small port interface, and the Android calls in a thin class behind it. BLE does this
   with `BleTransport` over `BleRadio`, and `AndroidBleRadio` as the only file that touches
   `android.bluetooth`. The logic then runs on the JVM against a fake radio.

3. **Pass the contract.** The adapter's tests extend `MeshTransportContract` (in `core/mesh`'s test
   fixtures) by building two transports in range:

   ```kotlin
   class BleTransportContractTest : MeshTransportContract() {
       override fun CoroutineScope.inRange(): Pair<MeshTransport, MeshTransport> {
           val air = FakeAir()
           return BleTransport(air.radio(), Random(1), context = coroutineContext.minusKey(Job)) to
               BleTransport(air.radio(), Random(2), context = coroutineContext.minusKey(Job))
       }
   }
   ```

   Pass the test's coroutine context without its `Job`, so the transport's loops run on virtual time
   but are not children the test waits for.

4. **Test hostile peers.** A bad length, a link that never speaks, a flood: each must drop that link
   and leave the phone on the mesh. Never allocate from a length read off the air. See
   `BleTransportTest` and `FramingTest`.

5. **Never crash the app.** A radio error ends one loop, not the process. `BleTransport` runs its
   loops under a `SupervisorJob` with a silent `CoroutineExceptionHandler`, catches `IOException`
   and `SecurityException` where the radio throws them, and retries a session that cannot start.

6. **Declare permissions in the adapter's manifest.** The merged manifest carries them into the app.
   `checkManifestPermissions` fails the build if any variant asks for `INTERNET`, which every
   socket-based radio (Wi-Fi Direct, Wi-Fi Aware) needs: such a transport needs an ADR first
   (ADR-0003). Expose the runtime permissions the radio needs (BLE: `AndroidBleRadio.PERMISSIONS`)
   so the app can ask for them in context.

7. **Register it.** `app/src/<variant>/kotlin/.../TransportRegistration.kt` is the only app file
   allowed to import an adapter. It returns a `Registration`: the transports, whether the only one is
   simulated, and the permissions to ask for. Release registers real radios only; debug registers
   the same and falls back to the simulated loopback radio on a phone without the hardware.

8. **Run the checks** (the validation matrix in `AGENTS.md`):

   ```bash
   ./gradlew :adapters:ble:test :adapters:ble:lint
   ./gradlew checkTransportBoundaries checkCryptoBoundaries checkCompressionImports
   ./gradlew checkForbiddenDependencies checkManifestPermissions
   node .ai/rungs.mjs check
   ```

9. **Prove it on phones.** Unit tests prove the logic; only phones prove the radio. Record each run
   in the adapter's work item: discovery time, round trips, survival with the screen off, and the
   OEM battery manager.

## 6. What the gates check

| Gate | Fails when |
| --- | --- |
| `checkTransportBoundaries` | `app/` or `core/` imports an adapter outside `TransportRegistration.kt`, or one adapter imports another |
| `checkCryptoBoundaries` | anything outside `core/crypto` imports a cryptographic API, `java.security` included |
| `checkManifestPermissions` | a merged manifest asks for `INTERNET` |
| `checkForbiddenDependencies` | a runtime classpath holds a proprietary or tracking library (Play Services, Nearby Connections, Firebase…) |
| `AdvertisementPrivacyTest` (BLE) | an advertisement may carry the device name or anything that names the phone |

## 7. Radio facts from the reference phones

Measured in the BLE spike (WI-007) and the first Echo run (WI-011), November 2026, phones on USB.

| | Pixel 9a | Pixel 11 Pro | OnePlus Nord CE 3 Lite |
| --- | --- | --- | --- |
| Android | 17 | 17 | 14 (OxygenOS) |
| Extended and periodic advertising, 2M and Coded PHY | yes | yes | yes |
| Max advertising data | 1,650 bytes | 1,650 bytes | 1,650 bytes |
| Discovery after start | 175 ms | 261 ms | 480 ms |
| L2CAP round trip, 150 bytes each way | 37 ms (with the 11 Pro), 95 ms (with the OnePlus) | 37 ms | 95 to 102 ms |
| Foreground service survived 15 to 43 min screen off | yes | yes | yes, OxygenOS did not kill it |

- **L2CAP CoC is the data path**: three times faster than GATT on the Pixels, and one channel stayed
  open through Doze. GATT remains the fallback for a phone where a channel does not open.
- **Legacy advertising for discovery**: found every peer in under 1.3 s. Extended advertising is on
  all three phones but not required.
- **OxygenOS refuses `pm grant` and `settings put` over adb** without a developer option: grant
  permissions by hand when testing on it.
- **Echo end to end**: a report reached the other Pixel about 7.5 s after Send, 5 s of it the
  cancel grace (AC-4).
- Not yet measured: a run off the cable (USB can hold a phone out of deep sleep), screen-off delivery
  in Echo itself (AC-5), and three phones in a line (AC-6a).
