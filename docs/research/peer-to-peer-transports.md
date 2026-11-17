# Phone-to-phone radio transports on Android: research report

*Protest map project · transport research thread · 2026-11-16 · draft, to move into the app repo's `docs/research/` once it exists*

How Android phones running the app can find each other and exchange small signed reports with no server, no internet and no Google Play Services. It covers Bluetooth LE (advertising, GATT, L2CAP), Wi-Fi Direct and Wi-Fi Aware: discovery, range, throughput, battery, background limits and permissions per Android version. It then compares relay strategies for a dense crowd and surveys prior art (Briar, Berty, Meshtastic, Bridgefy, FireChat) for what to reuse and what to avoid. It ends with a recommended MVP transport, a sketch of the capability contract for `:core:mesh`, and the measurements to run on the reference phones.

> **Confidence note.** Claims with a link were checked against that source on 2026-11-16. Claims marked ⚠️ come from general platform knowledge or third-party write-ups and **must be measured on the reference phones before we rely on them**. Range, throughput and battery figures are deliberately not given as numbers unless a source states them: radio performance in a crowd of bodies (2.4 GHz is absorbed by water, so by people) is exactly what our own tests must establish. The reference phones are not chosen yet; section 7 lists what to measure on them.

---

## 0. TL;DR and recommendations

| | **BLE advertising** | **BLE GATT / L2CAP** | **Wi-Fi Direct (P2P)** | **Wi-Fi Aware (NAN)** |
|---|---|---|---|---|
| Availability at minSdk 31 | All phones; extended adv needs chip support | GATT and L2CAP CoC on all phones | All phones ⚠️ | Only with chip support |
| Device coverage | Every phone we'd target | Every phone we'd target | Almost every phone ⚠️ | **Partial**: Pixels, recent Samsung flagships; many mid-range phones lack it ⚠️ |
| Connection needed | No (broadcast) | Yes, point-to-point | Yes, group with one owner | Discovery: no; data path: yes |
| Payload | 31 B legacy ⚠️; up to 1650 B extended ([AOSP](https://source.android.com/docs/core/connect/bluetooth/ble_advertising)) | Streams; slow | Full IP sockets, fast | ~255 B discovery messages ([Android](https://developer.android.com/develop/connectivity/wifi/wifi-aware)); IP data paths, fast |
| User dialogs | None | None | Connection invitation prompt ⚠️ | None |
| Background (with a foreground service) | Yes | Yes ([Android](https://developer.android.com/develop/connectivity/bluetooth/ble/background)) | Unreliable ⚠️ | Unknown ⚠️ |
| Air-visible identifier | Rotating random address ⚠️ + whatever we put in the payload | Same | Device name and P2P MAC ⚠️ | NAN MAC, randomized every 30 min by default ([AOSP](https://source.android.com/docs/core/connect/wifi-aware)) |
| Permissions (target 33+) | BLUETOOTH_SCAN (neverForLocation), BLUETOOTH_ADVERTISE | + BLUETOOTH_CONNECT | NEARBY_WIFI_DEVICES (neverForLocation) **and Location Mode on** | NEARBY_WIFI_DEVICES (neverForLocation); Wi-Fi and Location on |
| Role in this app | **Discovery + small broadcasts (MVP)** | **Sync of missing reports (MVP)** | Not planned | **Second transport, opportunistic bulk sync** |

**Recommendations**

1. **MVP transport is Bluetooth LE only**, as one adapter (`:adapters:ble`) that does two things: it *advertises* only a rotating ephemeral token, and it *connects* briefly (L2CAP, GATT as fallback) to peers it hasn't synced with recently, to swap inventories (blinded per connection) and the missing reports. BLE is the only option that works on every phone, needs no user dialog, keeps running in a foreground service, and leaves Wi-Fi alone.
2. **Do not plan on putting whole reports in advertisements.** A signed report does not fit in 31 bytes (an Ed25519 signature alone is 64 bytes), and extended advertising is chip-dependent on both the sending and the receiving phone. Use extended advertising as an *optimisation* when `isLeExtendedAdvertisingSupported()` is true on both ends, never as the baseline.
3. **Wi-Fi Aware is the second transport**, gated on `FEATURE_WIFI_AWARE` at runtime. It gives far more throughput with no dialogs and has OS-level MAC rotation, but device coverage is too patchy to be the baseline. It is the right home for later bulk features (map tiles exchange, photos if ever in scope).
4. **Drop Wi-Fi Direct from the plan** unless reference-phone testing contradicts the issues below: on Android 13+ it still needs Location Mode on for discovery ([Android](https://developer.android.com/develop/connectivity/wifi/wifip2p)), connections are a one-owner group model that does not suit a churning mesh, a phone typically holds one group at a time ⚠️, invitations show a system prompt ⚠️, and it is what FireChat-era and Berty-era apps reached for and then struggled with.
5. **No Google Nearby Connections.** It would solve much of this but is part of Play Services (ADR constraint 3). Briar, the closest trustworthy prior art, ships on F-Droid as GPL-3.0-or-later ([F-Droid](https://f-droid.org/packages/org.briarproject.briar.android/)) without it, which shows the constraint is workable.
6. **Relay strategy: epidemic gossip with anti-entropy, not blind flooding.** Every report has a content-hash ID, an absolute expiry time and a hop budget. Phones exchange inventories inside a connection and pull only what they lack, verify the signature *before* storing or relaying, drop anything expired, over budget or over size, and rate-limit per peer. This is the direct answer to Bridgefy's "forward before parsing" shutdown and to crowd-scale broadcast storms. Details in section 5.
7. **Every adapter runs inside one `connectedDevice` foreground service**, started while the app is visible. Android 14+ accepts BLUETOOTH_SCAN/ADVERTISE/CONNECT or CHANGE_WIFI_STATE as the prerequisite for that type ([Android](https://developer.android.com/develop/background-work/services/fgs/service-types)).

---

## 1. Cross-cutting Android concerns

These apply to every adapter and belong in `:core:mesh` or the app's service layer.

### 1.1 Permissions per Android version (minSdk 31, decided 2026-11-16)

Antoine set minSdk to 31 (Android 12), so the Android 8–11 path, where any Bluetooth app had to hold precise (and on 10–11 background) location, no longer applies.

| Android version | Bluetooth LE | Wi-Fi Aware / Wi-Fi Direct |
|---|---|---|
| 12 (API 31–32) | BLUETOOTH_SCAN (`neverForLocation`), BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT, all runtime, shown as one "Nearby devices" prompt ([Android](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)). No location permission | Still **ACCESS_FINE_LOCATION** (runtime), plus ACCESS_WIFI_STATE, CHANGE_WIFI_STATE, CHANGE_NETWORK_STATE |
| 13+ (API 33+) | Same as 12 | **NEARBY_WIFI_DEVICES** with `neverForLocation`; ACCESS_FINE_LOCATION gets `maxSdkVersion="32"` ([Android](https://developer.android.com/develop/connectivity/wifi/wifi-permissions)) |

Consequences for the product:

- **The BLE MVP needs no location permission on any supported phone.** Only the Wi-Fi Aware adapter on Android 12 (API 31–32) still needs ACCESS_FINE_LOCATION; the app should simply not offer that transport on 12 unless the user grants it, and say why.
- `neverForLocation` on BLUETOOTH_SCAN "filters some BLE beacons from the scan results" ([Android](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)). Our own advertisements use a custom service UUID, so this should not affect us ⚠️ verify.
- Wi-Fi Direct discovery needs Location Mode switched on even on 13+ ([Android](https://developer.android.com/develop/connectivity/wifi/wifip2p)); Wi-Fi Aware is unavailable "because the user has disabled Wi-Fi or Location" ([Android](https://developer.android.com/develop/connectivity/wifi/wifi-aware)). Asking a protester to turn on Location services is a privacy cost the UI must name honestly. BLE on 12+ needs no Location Mode ⚠️ verify on reference phones.
- Bluetooth and Wi-Fi share the "Nearby devices" permission group, so "users can't disable Wi-Fi access but keep Bluetooth access enabled" ([Android](https://developer.android.com/develop/connectivity/wifi/wifi-permissions)). One prompt covers both transports on 13+.

### 1.2 Background execution, Doze and foreground services

- In Doze the system "suspends network access", "ignores wake locks" and "doesn't perform Wi-Fi scans" ([Android](https://developer.android.com/training/monitoring-device-state/doze-standby)). An app with a foreground service is exempt from App Standby (same source). Whether BLE scanning and advertising keep running in Doze while a foreground service holds them is not stated by the docs ⚠️; in practice they do on stock Android, and OEM battery managers are the bigger risk (next point).
- BLE has "no limitation" on scanning or connecting "while the app is not visible", but the process must be alive ([Android](https://developer.android.com/develop/connectivity/bluetooth/ble/background)). The process stays alive by running a foreground service.
- Android 14+ requires a declared type. **`connectedDevice`** fits: its prerequisite is a granted BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE or BLUETOOTH_SCAN, or a manifest-declared CHANGE_WIFI_STATE / CHANGE_NETWORK_STATE ([Android](https://developer.android.com/develop/background-work/services/fgs/service-types)). The type permission (FOREGROUND_SERVICE_CONNECTED_DEVICE) is a normal permission, and the runtime prerequisites must be granted *before* `startForeground()` or it throws ([Android](https://developer.android.com/about/versions/14/changes/fgs-types-required)).
- Start the service while the app is in the foreground (user taps "Join the mesh"). Background-start restrictions since Android 12 make starting it later unreliable ⚠️. Never start it from BOOT_COMPLETED; the app should be off the air until the user deliberately turns it on, which is also the right privacy default.
- **OEM battery killers** (Samsung "sleeping apps", Xiaomi, Huawei and others) kill foreground services that stock Android would keep ⚠️. The `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` route exists but is unverified here and has its own policy history. The "device support" issue template in the brief exists for exactly this; the onboarding should link per-OEM instructions.
- Android discourages scanning in a loop: "Never scan on a loop, and always set a time limit on your scan" ([Android](https://developer.android.com/develop/connectivity/bluetooth/ble/find-ble-devices)). Android also throttles apps that start scans too often (about 5 starts per 30 seconds) ⚠️. The BLE adapter needs a duty cycle (for example scan windows with low-power settings between bursts) tuned by measurement, and a long-running filtered scan rather than repeated starts.

### 1.3 Identifiers on the air

The brief's rule is "no stable identifier ever goes over the air" and "rely on the OS's MAC randomization rather than defeating it".

- **BLE:** Android advertises from a resolvable or non-resolvable random address that rotates ⚠️ (commonly about every 15 minutes, and on each new advertising set ⚠️ verify). Anything *we* put in the payload must rotate at least as often, ideally in lock-step with the address, otherwise the payload links the old and new address. Bridgefy failed exactly here: its advertisement carried the CRC32 of a user ID that "does not change unless the user reinstalls" ([Albrecht et al. 2021](https://eprint.iacr.org/2021/214)). Restarting the advertising set when we rotate our token should also rotate the address ⚠️ verify.
- **Wi-Fi Aware:** NAN management and data interface MACs are randomized "at a regular interval … by default 30 minutes", possibly less often while a data path is up ([AOSP](https://source.android.com/docs/core/connect/wifi-aware)). Our service-specific info must rotate on the same schedule.
- **Wi-Fi Direct:** broadcasts a device name (by default the phone's model or user-set name) and a P2P MAC whose randomization varies by version and OEM ⚠️. Another reason to leave it out.
- **Bluetooth adapter name** is not sent in our BLE advertisements as long as we never call `setIncludeDeviceName(true)`. Add a lint/gate for that. **But** when another phone connects to our GATT server, Android's built-in Generic Access service exposes the adapter name in its Device Name characteristic to any connected central ⚠️ verify on the reference phones. The app cannot remove that service without privileged access. Options: connect only over L2CAP (no GATT server running, so no GATT read; but the L2CAP PSM then has to travel in the advertisement), and/or tell the user in onboarding to set a generic Bluetooth name. Test item 9.
- **Our own address is not readable by the app.** Reading the advertising set's own address needs a privileged permission ⚠️, so the app cannot check in-process that a restart produced a new address. Rotation has to be verified from a second phone (test item 7).
- **Choosing the address type** (`AdvertisingSetParameters.Builder.setOwnAddressType`, for example random non-resolvable) is API 34+ ⚠️. On Android 12–13 we get whatever the stack picks.
- **The OS may rotate a running set's address on its own timer** (≈15 min ⚠️) not aligned with ours. Rotating our payload by stopping and restarting the set every ≤10 min should keep us ahead of that timer, but if the stack ever rotates mid-set, one payload is briefly seen under two addresses. Test item 7 must check that too.
- **When we act as central** (scanning and connecting), the connection request carries the phone's system-wide private address, which rotates on the OS schedule, not ours, and is shared with any other BLE use on the phone ⚠️. Active scanning also sends scan requests from that address ⚠️; the public scan API offers no passive-scan switch ⚠️ verify. Neither carries app data, but a sniffer can correlate central and peripheral activity in time.

Even with all of this, an observer with radio sniffers learns *that* a phone running the app is present (our service UUID is constant). Hiding app use entirely is out of reach for any transport here; the threat-model thread must state it as a residual risk.

### 1.4 Coexistence

Bluetooth and Wi-Fi share the 2.4 GHz radio front-end on most phones and time-share it ⚠️. Running a BLE scan, BLE advertising and a Wi-Fi Aware cluster at once costs throughput on each. Wi-Fi Direct may also disconnect or degrade the phone's normal Wi-Fi on some devices ⚠️.

---

## 2. Bluetooth LE

### 2.1 Advertising and scanning (discovery and small broadcast)

- Legacy advertising carries 31 bytes of payload ⚠️ (the AOSP page shows extended sets up to 251 bytes per packet, 1650 total, but does not state the legacy limit). After flags and a 128-bit service UUID, roughly 10–12 bytes remain for our data ⚠️ compute exactly. A 16-bit UUID would save 14 bytes but needs SIG registration; service data under a 128-bit UUID is the free option.
- Android 8.0 (below our minSdk) added Bluetooth 5 features: 2M PHY, LE Coded PHY (long range), extended and periodic advertising, each behind a runtime check (`isLe2MPhySupported()`, `isLeCodedPhySupported()`, `isLeExtendedAdvertisingSupported()`, `isLePeriodicAdvertisingSupported()`), and extended data "goes up to 1650 bytes" via `getLeMaximumAdvertisingDataLength()` ([AOSP](https://source.android.com/docs/core/connect/bluetooth/ble_advertising)).
- **LE Coded PHY** trades speed for range (the Bluetooth 5 "4× range" claim is for ideal conditions ⚠️). Many phones report it as supported but scanning coded-PHY advertisements from other phones has been unreliable in developer reports ([Nordic DevZone](https://devzone.nordicsemi.com/f/nordic-q-a/70057/scanning-of-phy-coded-advertising-data-on-android/287264), [Google issue tracker](https://issuetracker.google.com/issues/148487283)). Worth testing, not worth depending on.
- Background-friendly scanning: `startScan()` with a `PendingIntent` delivers matches even when the app is not running, but needs filters ([Android](https://developer.android.com/develop/connectivity/bluetooth/ble/background)). We need a filter on our service UUID anyway.
- How many advertising sets a phone can run at once is not documented and varies ⚠️. Use one set and rotate its contents.

**What goes in a legacy advertisement:** a protocol version byte and a random ephemeral token (rotated with the address, at most every 10 min). The threat model (§5.1) forbids putting any inventory or digest of held reports in the advertisement, because the set of reports a phone holds is a fingerprint. A scanning phone therefore cannot tell from the advertisement whether a peer has anything new; it connects to tokens it hasn't synced with recently, with a cap on connection attempts per minute, and does the inventory exchange inside the connection. During a group join, the member's phone also advertises a short value derived from the QR's rendezvous value (≤8 bytes fits; the full 128-bit value plus a 128-bit service UUID does not fit in 31 bytes ⚠️ compute exactly). Neither of these is enough to carry a report.

**What fits in an extended advertisement:** a full compact signed report (ID, type, coarse position, expiry, hop budget, Ed25519 signature ≈ 110–150 bytes in a binary format ⚠️ estimate) or several. Connectionless dissemination is very attractive in a crowd because it scales without connections, but both sides need extended advertising support. Plan it as a capability (`canBroadcast(maxBytes)`) that the mesh uses when present.

### 2.2 GATT and L2CAP (moving the reports)

- BLE needs no pairing, which is why Berty chose it as the cross-platform bridge, but it "is extremely slow" and "totally unsuitable for exchanging photos" ([Berty, 2020](https://berty.tech/blog/bluetooth-low-energy/)). For reports of ~150 bytes, slow is fine: hundreds of reports per connection are feasible ⚠️ measure.
- GATT: one side runs a GATT server with a write characteristic and a notify characteristic, the other connects as client. Request a larger MTU (up to 517) after connecting ⚠️; Android 14 is reported to request 517 automatically ⚠️ verify. Unbonded connections give no link-layer encryption, which is fine because all our payloads are signed and group data is end-to-end encrypted at the application layer.
- L2CAP connection-oriented channels (`listenUsingInsecureL2capChannel()`, API 29 ⚠️) give a socket-like stream with better throughput than GATT and no characteristic juggling. The dynamic PSM must be learned out-of-band (put it in the advertisement or a GATT characteristic) ⚠️. With minSdk 31 every phone has the API, so L2CAP is the default; keep GATT as a fallback for stacks where L2CAP proves unreliable on test ⚠️.
- Android BLE stacks are buggy: Berty reports "race condition issues that can occur at a low level that are not directly handled by the Android APIs" and unstable connections ([Berty, 2020](https://berty.tech/blog/bluetooth-low-energy/)). The adapter needs a strict single-operation GATT queue, timeouts on every call, `gatt.close()` on every path, and a cap on concurrent connections (phones commonly support about 4–7 ⚠️). Short-lived connections (connect, sync, disconnect) are more robust in a churning crowd than holding links open.
- Both phones must be able to play both roles (central and peripheral). Android supports peripheral mode since 5.0 on capable hardware; check `isMultipleAdvertisementSupported()` / `getBluetoothLeAdvertiser() != null` ⚠️. Phones that cannot advertise can still scan and connect, as listen-mostly peers.
- Connection collision: when two phones see each other, both may try to connect. Break the tie deterministically (for example the lower ephemeral token initiates) ⚠️ design detail for `:core:mesh`.

### 2.3 Range, throughput and battery

No source we checked gives numbers that would hold in a crowd. Bluetooth class 2 phone radios are commonly quoted at "about 10 m indoors, a few tens of metres outdoors" ⚠️, and human bodies attenuate 2.4 GHz heavily. In a dense crowd *that is acceptable*: density means there is always a phone within a few metres, so the mesh trades per-hop range for hop count. Battery cost depends almost entirely on scan duty cycle; continuous low-latency scanning is the expensive part ⚠️. Section 7 lists the measurements.

---

## 3. Wi-Fi Aware (Neighbor Awareness Networking)

- The API exists on every Android version we support (it arrived in 8.0), but only on devices with the hardware feature; check `hasSystemFeature(FEATURE_WIFI_AWARE)` and then `WifiAwareManager.isAvailable()`, and listen for `ACTION_WIFI_AWARE_STATE_CHANGED` because availability changes at runtime ([Android](https://developer.android.com/develop/connectivity/wifi/wifi-aware)).
- **Discovery** is publish/subscribe with service-specific info and messages "limited to about 255 bytes" (`getMaxServiceSpecificInfoLength()`), and messages "might not be delivered (or be delivered out-of-order or more than once)" ([Android](https://developer.android.com/develop/connectivity/wifi/wifi-aware)). 255 bytes holds one compact signed report, so Wi-Fi Aware discovery alone could carry reports between nearby phones without a data path.
- **Data paths** give an IPv6 link with ordinary sockets and "higher throughput rates across longer distances than Bluetooth" (same source, no figures). Concurrent data paths are limited by hardware: `getAvailableAwareResources()` reports them on Android 12+; a third-party test reports 2 on a Pixel 4a and up to 8 on Pixel 7+ ([grjte, hackmd](https://hackmd.io/@grjte/cross-platform-wifi-aware)) ⚠️.
- **Battery:** "As long as there are active sessions, the system maintains synchronization with a Wi-Fi Aware cluster. This clustering consumes resources and battery" ([Android](https://developer.android.com/develop/connectivity/wifi/wifi-aware)). Measure.
- **Device coverage** is the weak point. Pixels from Pixel 2 onwards and Samsung phones "from the past several years" per a third-party write-up ([grjte, hackmd](https://hackmd.io/@grjte/cross-platform-wifi-aware)); no authoritative list exists. The Wi-Fi Alliance certified-product finder and Ditto's open-source [wifi-aware-checker](https://github.com/getditto/wifi-aware-checker) are the ways to check a given phone. Expect many budget phones, the ones protesters are likely to carry, not to have it ⚠️.
- **iOS:** iOS 26 added Wi-Fi Aware, but the same write-up reports that cross-platform links are "still impossible" because iOS requires NAN v4 pairing that Android phones do not yet ship ([grjte, hackmd](https://hackmd.io/@grjte/cross-platform-wifi-aware)) ⚠️ undated. iOS is out of scope for the MVP anyway; note it for later.
- Background behaviour with the screen off is not documented ⚠️ measure.

---

## 4. Wi-Fi Direct (Wi-Fi P2P)

Kept for completeness and to justify leaving it out.

- `WifiP2pManager` methods `discoverPeers`, `connect`, `createGroup`, `discoverServices` and others need NEARBY_WIFI_DEVICES on 13+ and ACCESS_FINE_LOCATION on 12; `discoverPeers()`, `discoverServices()` and `requestPeers()` "also require Location Mode to be enabled" ([Android](https://developer.android.com/develop/connectivity/wifi/wifip2p)).
- A connection forms a group with one "group owner" acting as the access point ([Android](https://developer.android.com/develop/connectivity/wifi/wifi-direct)). A phone is generally in at most one group at a time ⚠️, so it cannot bridge two groups without leaving one: poor for a mesh.
- The first connection between two phones shows a system invitation dialog the user must accept ⚠️ (not stated on the Android page; widely reported). That alone rules it out for passive relaying in a pocket.
- DNS-SD service discovery over P2P exists ([Android](https://developer.android.com/develop/connectivity/wifi/nsd-wifi-direct), not read in full) but needs the same Location Mode.
- Its strengths (high throughput, near-universal support) matter only for bulk transfers that are out of the MVP.

---

## 5. Relay strategies for a dense crowd

### 5.1 The problem

Thousands of phones in a few hundred metres, each seeing dozens of peers, peers churning every minute, reports that matter for minutes. The two failure modes are (a) reports not reaching people 300 m away fast enough, and (b) the radio channel collapsing under redundant retransmissions (a broadcast storm), plus the adversarial version of (b): a flooder or a single crafted message knocking phones off.

### 5.2 Options

| Strategy | How it works | Fit |
|---|---|---|
| **Blind flooding with TTL** | Every node rebroadcasts every new packet until a hop counter hits zero. Bridgefy: "managed flood-based mesh network, preventing infinite loops using a time-to-live counter" ([Albrecht et al. 2021](https://eprint.iacr.org/2021/214)) | Simple, but redundant transmissions grow with density; Bridgefy forwarded "any payload before parsing it", so one zip-bomb message "could completely disable the mesh network" (same source) |
| **Managed flooding** (Meshtastic) | Rebroadcast up to a hop limit (default 3, max 7, [Meshtastic](https://meshtastic.org/docs/configuration/radio/lora/)); before rebroadcasting a node "listens a short while to see if another node has already rebroadcasted the packet. If so, it will not rebroadcast", with a delay that is shorter for weaker signals so farther nodes go first ([Meshtastic](https://meshtastic.org/docs/overview/mesh-algo/)) | Good idea for a shared broadcast medium (LoRa, BLE extended advertising). Meshtastic chose it because most traffic is broadcast and it "eliminates the need for route discovery" ([Meshtastic blog](https://meshtastic.org/blog/why-meshtastic-uses-managed-flood-routing/)), which is also true for us |
| **Epidemic gossip with anti-entropy** | On contact, peers exchange a summary of what they hold and pull only what the other lacks. Store-and-forward: a phone that walks across the crowd carries reports with it | Best fit for connection-based links (BLE GATT/L2CAP, Wi-Fi Aware data paths). Redundancy is bounded by the inventory exchange instead of by luck. This is what Briar-style sync does between contacts ⚠️ (Briar syncs only between contacts, not strangers) |
| Routing (AODV, next-hop) | Discover and maintain paths | Wrong for us: topology changes faster than routes converge, and almost all traffic is broadcast. Meshtastic only uses next-hop for direct messages, with flooding as fallback |

### 5.3 Recommended design for `:core:mesh`

A hybrid, tested on the loopback transport with simulated crowds before any radio work:

1. **Report identity.** ID = hash of the canonical signed report. Dedupe on ID everywhere. Bridgefy's papers describe a hop counter and a `track` list of relaying user IDs but no message-ID dedupe ([Albrecht et al. 2022](https://www.usenix.org/conference/usenixsecurity22/presentation/albrecht)); our relay path must carry **no** relayer identity at all.
2. **Two budgets per report:** an absolute expiry time (signed, the authoritative limit, drives "reports expire") and a hop budget (unsigned, decremented on relay, caps spread in space). Expired or zero-budget reports are not relayed.
3. **Verify, then store, then relay.** Parse with a strict size cap *before* any decompression (better: no compression at all on the wire), check version, type, expiry and signature, and only then store or forward. A malformed message costs one bounded parse and is dropped. Never relay what you can't verify. (Group-encrypted reports that strangers relay but can't read need an outer signature or authenticated envelope that strangers can check; that is a question for the threat-model thread.)
4. **Anti-entropy on connection.** Advertise no inventory (threat model §5.1). Inside a connection, exchange a compact set summary with ids blinded by a per-connection random salt (sorted ID list for small sets, a Bloom filter or range-based set reconciliation for large ones), then send missing reports newest and most severe first, within a per-connection byte budget.
5. **Broadcast where the link allows.** When extended advertising (or Wi-Fi Aware discovery messages) is available on both ends, also push the newest few reports connectionless, with Meshtastic-style "listen, then rebroadcast only if nobody else did" suppression. Caveats from the threat model: a broadcast frame is not an inventory, so §5.1 allows it, but the same frame re-advertised by one phone across an address rotation weakly links the two addresses (weak because many phones carry the same public report). Group frames are padded to one fixed size of roughly 100–150 bytes of overhead plus payload; that fits extended advertising (up to 1650 B) and, only if the padded size stays under ~255 B, a Wi-Fi Aware discovery message. Over L2CAP or an Aware data path the size is unconstrained. Post-MVP either way.
6. **Per-peer and global rate limits, bounded storage.** Cap reports accepted per ephemeral peer per minute, cap total stored reports, and evict by expiry then by fewest confirmations. A flooder must exhaust its own budget, not ours. Ephemeral tokens rotate, so limits also need to be per-connection and global.
7. **Confirmations aggregate**, rather than spawning new full reports, so "+1 this tear-gas report" does not multiply traffic.

The fuzzing requirement in the brief applies to the decoder in step 3; it is the single most important defence.

### 5.4 Evidence gap

We found no published measurements of BLE gossip propagation in real protest crowds. The 2021 Bridgefy paper notes Bluetooth Mesh is designed for managed networks, the opposite of a crowd's "unpredictable and always-changing flow" ([Albrecht et al. 2021](https://eprint.iacr.org/2021/214)), but measures security, not propagation. Our loopback simulator plus a field test with a handful of phones will be the first evidence; we should not claim propagation speeds in the UI until we have it.

---

## 6. Prior art: what to reuse, what to avoid

| Project | Transports | License, Play Services | Reuse | Avoid |
|---|---|---|---|---|
| **Briar / Bramble** | "sync via Bluetooth, Wi-Fi or memory cards" offline, Tor online ([Briar](https://briarproject.org/how-it-works/)); Bluetooth classic RFCOMM and LAN TCP ⚠️ (spec pages unreachable from here) | GPL-3.0-or-later on F-Droid ([F-Droid](https://f-droid.org/packages/org.briarproject.briar.android/)); no Play Services ⚠️ | Published protocol specs (BTP, BHP, BQP for QR key agreement) in [briar-spec](https://code.briarproject.org/briar/briar-spec); QR in-person key exchange matches our group-join flow; years of Android Bluetooth hardening. License is compatible with ours | Classic Bluetooth RFCOMM needs discoverable mode or prior pairing ⚠️; Briar syncs between contacts only, no relaying for strangers ⚠️, so its sync model does not cover crowd-wide reports |
| **Berty / Wesh** | BLE (cross-platform), Android Nearby, Apple Multipeer; "Wi-Fi direct connection instead of BLE which is much faster" ([Berty protocol](https://berty.tech/protocol)) | Apache-2.0 / MIT ([GitHub](https://github.com/berty/berty)); Android Nearby is a Play Services API | Their write-up of Android BLE pitfalls; Apache/MIT code is GPL-3.0-compatible if we borrow BLE driver code | Android Nearby (proprietary); protocol "has still not been thoroughly audited" and the README says not to use it for sensitive data yet |
| **Meshtastic** | LoRa radios (separate hardware) | GPL-3.0 ⚠️ | Managed-flooding algorithm and hop-limit defaults (section 5) | Not phone-to-phone radio; needs dedicated hardware |
| **Bridgefy** | BLE mesh, proprietary SDK | Proprietary | Nothing but lessons | Stable ID in adverts, plaintext sender/receiver IDs, forward-before-parse, decompression bomb, unauthenticated broadcasts ([2021](https://eprint.iacr.org/2021/214), [2022](https://www.usenix.org/conference/usenixsecurity22/presentation/albrecht)) |
| **FireChat** | Bluetooth, Wi-Fi, Apple Multipeer; used in the 2014 Hong Kong protests; developers said it "was not meant for secure or private communications"; discontinued, no updates since 2018 ([Wikipedia](https://en.wikipedia.org/wiki/FireChat)) | Proprietary | That demand exists | Closed code, no security story, single company that disappeared |

---

## 7. Measurements to run on the reference phones

Each becomes a work item once the repo exists and the phones are chosen. Record results in this file, per phone model and Android version.

1. Feature matrix per phone: extended advertising, LE Coded PHY, 2M PHY, max advertising data length, peripheral mode, L2CAP CoC, `FEATURE_WIFI_AWARE`, available Aware data paths.
2. BLE discovery latency and range, phone-to-phone, in open air and with bodies in between (two people standing in the line of sight).
3. GATT vs L2CAP throughput for a batch of 100 compact reports.
4. Extended-advertising and coded-PHY reception between the two phones in both directions.
5. Battery drain per hour with the mesh service running at two or three scan duty cycles, screen off.
6. Does the foreground service keep scanning, advertising and accepting connections after 30 min and 2 h screen-off (Doze), and with the OEM battery manager at default settings?
7. Address rotation: log our own advertising address over an hour and confirm it rotates when we restart the advertising set.
8. Wi-Fi Aware discovery-message delivery rate and data-path throughput, if both phones support it.
9. With phone A running a GATT server and phone B connected unbonded, can B read A's Bluetooth device name from the Generic Access service? Repeat with L2CAP only and no GATT server.
10. Group join: time from QR scan to a finished Noise handshake over one L2CAP hop, and failure rate at 1 m and 5 m in a crowd-like setting (threat model open question 4).

---

## 8. Proposed transport capability contract (sketch for ADR-0002)

The UI and core know capabilities, never transport names (brief, constraint 4). A first cut for `:core:mesh`:

```kotlin
interface MeshTransport {
    val capabilities: TransportCapabilities
    val state: StateFlow<TransportState>            // Off, Unavailable(reason), Starting, Running
    val peers: Flow<PeerEvent>                       // Seen(ephemeralPeer), Lost
    val broadcasts: Flow<ReceivedFrame>              // connectionless frames, if canBroadcast

    suspend fun start(advertisement: AdvertisementPayload)
    suspend fun stop()
    suspend fun updateAdvertisement(advertisement: AdvertisementPayload)  // rotation (restarts the set) and join rendezvous
    suspend fun broadcast(frame: ByteArray)          // only if capabilities.maxBroadcastBytes > 0
    suspend fun open(peer: EphemeralPeer): PeerLink  // byte stream with timeouts; caller closes
}

data class TransportCapabilities(
    val rangeClass: RangeClass,                      // Short, Medium
    val bandwidthClass: BandwidthClass,              // Low (BLE), High (Wi-Fi)
    val maxAdvertisementBytes: Int,                  // e.g. ~11 legacy BLE, ~255 Aware
    val maxBroadcastBytes: Int,                      // 0 when no connectionless broadcast
    val maxConcurrentLinks: Int,
    val worksInBackground: Boolean,
    val needsLocationMode: Boolean,                  // drives honest onboarding copy
    val rotatesRadioAddress: Boolean,
)
```

`EphemeralPeer` carries only the current rotating token and a transport-internal handle; it is never persisted. `:adapters:loopback` implements the same interface in memory, with configurable loss, latency and range, so the gossip design in section 5 can be tested at crowd scale on the JVM before the BLE adapter exists.

---

## Sources

Android and AOSP documentation: [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions), [BLE advertising (AOSP)](https://source.android.com/docs/core/connect/bluetooth/ble_advertising), [Find BLE devices](https://developer.android.com/develop/connectivity/bluetooth/ble/find-ble-devices), [BLE in the background](https://developer.android.com/develop/connectivity/bluetooth/ble/background), [Wi-Fi Aware](https://developer.android.com/develop/connectivity/wifi/wifi-aware), [Wi-Fi Aware (AOSP)](https://source.android.com/docs/core/connect/wifi-aware), [Wi-Fi P2P](https://developer.android.com/develop/connectivity/wifi/wifip2p), [Wi-Fi Direct connections](https://developer.android.com/develop/connectivity/wifi/wifi-direct), [Wi-Fi permissions](https://developer.android.com/develop/connectivity/wifi/wifi-permissions), [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types), [Android 14 FGS types required](https://developer.android.com/about/versions/14/changes/fgs-types-required), [Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby).

Prior art and papers: [Albrecht, Blasco, Jensen, Mareková, "Mesh Messaging in Large-scale Protests: Breaking Bridgefy", CT-RSA 2021](https://eprint.iacr.org/2021/214); [Albrecht et al., "Breaking Bridgefy, again", USENIX Security 2022](https://www.usenix.org/conference/usenixsecurity22/presentation/albrecht) and [project page](https://eikendev.github.io/breaking-bridgefy-again); [Briar, how it works](https://briarproject.org/how-it-works/); [Briar on F-Droid](https://f-droid.org/packages/org.briarproject.briar.android/); [briar-spec](https://code.briarproject.org/briar/briar-spec); [Berty, BLE blog post (2020)](https://berty.tech/blog/bluetooth-low-energy/); [Berty protocol](https://berty.tech/protocol); [Berty on GitHub](https://github.com/berty/berty); [Meshtastic mesh algorithm](https://meshtastic.org/docs/overview/mesh-algo/); [Meshtastic LoRa config](https://meshtastic.org/docs/configuration/radio/lora/); [Why Meshtastic uses managed flood routing](https://meshtastic.org/blog/why-meshtastic-uses-managed-flood-routing/); [FireChat (Wikipedia)](https://en.wikipedia.org/wiki/FireChat); [Cross-platform Wi-Fi Aware (grjte, hackmd)](https://hackmd.io/@grjte/cross-platform-wifi-aware); Nordic DevZone threads on [coded PHY scanning on Android](https://devzone.nordicsemi.com/f/nordic-q-a/70057/scanning-of-phy-coded-advertising-data-on-android/287264); [Google issue 148487283](https://issuetracker.google.com/issues/148487283).
