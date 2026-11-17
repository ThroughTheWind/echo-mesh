# Echo — MVP Product Spec

| | |
|---|---|
| **Status** | v0.3 draft — transport research and threat model folded in; open questions in §10 |
| **App name** | Echo |
| **Package id** | `io.github.throughthewind.echo` |
| **Date** | 2026-11-16 |
| **Platform** | Android 12+ phones only (minSdk 31) |
| **License** | GPL-3.0-only (free, open source) |
| **Repository** | https://github.com/ThroughTheWind/echo-mesh |
| **First transport** | Bluetooth LE only (advertising for discovery, GATT/L2CAP to sync reports), per the [transport research](research/peer-to-peer-transports.md) |
| **Reference phones** | Google Pixel 11 Pro, Google Pixel 9a, OPPO Find X3 Pro (ColorOS, to test an OEM battery manager) |
| **Repo tooling** | `rungs` (`@rungs/cli`), as in Zapper |
| **Sibling project** | [Zapper](https://github.com/ThroughTheWind/zapper): same methodology, tooling and quality bar |

Threats and the crypto design: see [threat-model.md](threat-model.md). Radio transports and relay
strategy: see [research/](research/). Visual design: see the [design system](design/README.md).
Delivery order: see the [roadmap](ROADMAP.md). What comes after the MVP, and the 1.0 additions
(F15–F19) that join this spec's definition of done: see the [extended spec](SPEC-EXTENDED.md).

Related work running in parallel (this spec stays at product level and links to them for the details):
- **Transport research** — BLE, Wi-Fi Direct, Wi-Fi Aware, relay strategies, prior art. Tag **[DEP: transport]**.
- **Threat model and crypto design** — adversaries, protocol and library choice. Tag **[DEP: threat]**.
- **Design system** — street-ready visual language and components. Tag **[DEP: design]**.
- **Stack and repository setup** — build, modules, CI, gates, ADR-0001 to ADR-0003. Tag **[DEP: stack]**.

---

## 1. Vision

A free, open-source Android app that works like Waze for people at a protest. Phones nearby share
short-lived, map-pinned reports ("police line here", "tear gas", "medic", "water", "exit open",
"street closed") directly with each other, with no server and no internet, and reports hop from phone
to phone through the crowd. People who trust each other form an affinity group in person, share
encrypted reports, and can opt in to see each other on the map.

The people who use it may be targeted. **Security and privacy are the product, not a feature**, and
the app never claims more safety than it delivers.

### Why it exists

- The network is often cut exactly when people gather (Access Now #KeepItOn: at least 296 shutdowns
  in 54 countries in 2024, 74 of them tied to protests; 313 in 52 countries in 2025, a report that
  restates the 2024 total as 304).
- The best-known protest mesh app, Bridgefy, was shown to leak, to be trackable and to relay
  unauthenticated broadcasts (Royal Holloway 2020; *Breaking Bridgefy, again*, USENIX Security 2022).
  Those papers are the checklist of attacks this app is designed against. **[DEP: threat]**
- The trustworthy offline tools (Briar) are messengers, not maps.

Sources and further evidence: the transport research and threat model threads. Never invent figures.

### Goals for the MVP
1. **Situational awareness without a network.** A report dropped on one phone reaches phones several
   hops away within a minute in a crowd, and disappears when it expires.
2. **Trust you can see.** Every relayed message is authenticated. The map shows plainly whether a
   report came from your group (verified) or from someone nearby (unverified).
3. **A seized or sniffed phone gives away as little as possible.** No stable identifier on the air,
   no location history, encrypted storage, a fast panic wipe.
4. **Prove the transport architecture.** One radio transport ships behind a capability contract, and
   the whole mesh is tested as many simulated phones, including hostile ones, on the JVM.

### Non-goals for the MVP
- iOS, tablets, wearables, desktop.
- Any server, account, sign-in, internet relay or cloud sync.
- Chat, photos, video, voice, files (see §3.2 for why).
- Turn-by-turn navigation or route planning.
- Monetization, analytics, crash reporting, telemetry of any kind.

---

## 2. Users and personas

| Persona | Description | Primary need |
|---|---|---|
| **Marcher** (primary) | At a protest, alone or with friends, phone in pocket, network down or unreliable. | Glanceable map of what's happening nearby; drop a report in two taps. |
| **Affinity group member** | Came with 3–15 people who know each other; agreed a plan beforehand. | Private reports and seeing where the group is, without anyone else seeing it. |
| **Street medic / support** | Medic, legal observer, water or supply point. | Be findable by the people who need them, then disappear from the map when they move. |
| **Contributor** | Developer adding a transport or reviewing the crypto. | Clear transport contract, loopback mesh for tests, written threat model. |

**Adversaries** (anti-personas, defined in the threat model **[DEP: threat]**): a state with radio
sniffers, an infiltrator inside a group, someone who seizes a locked or unlocked phone, a flooder
injecting fake or malformed reports.

---

## 3. Scope

### 3.1 In scope (MVP)

| # | Area | Summary |
|---|---|---|
| F1 | Honest onboarding | First launch explains in plain words what the app protects and what it doesn't, then asks for permissions one at a time with reasons. |
| F2 | Offline map | Pick a region and get OpenStreetMap data for it, or import a map file; render fully offline. |
| F3 | Mesh presence | Join or leave the mesh; see peers nearby and time since last sync; relay in the background while on. |
| F4 | Drop a report | Choose a type, place it on the map (not automatically at your position), send. |
| F5 | Report spread | Reports hop phone to phone with a hop limit and de-duplication, and stop at expiry. |
| F6 | Confirm or dismiss | Confirm a report you can see is still true, or dismiss one that's gone; others see the counts. |
| F7 | Expiry | Every report has a short time-to-live and vanishes from every phone when it ends. |
| F8 | Affinity groups | Create a group and let others join in person by scanning a QR code; leave a group. |
| F9 | Group reports | Reports sent to a group are end-to-end encrypted and shown as verified. |
| F10 | Position sharing | Opt in, per group and per session, to share your live position with that group only. |
| F11 | Panic wipe | One deliberate action erases keys, groups, reports and settings in under 2 seconds. |
| F12 | Local protection | Encrypted storage, no location history, no plaintext logs. |
| F13 | Discreet mode | Dim, silent, neutral notification text; usable without drawing attention. **[DEP: design]** |
| F14 | Transport architecture | Core knows capabilities, not radios; one transport adapter ships, plus an in-memory loopback for tests. |

### 3.2 Out of scope (MVP), candidates for later

| Item | Why not now | Target |
|---|---|---|
| Wi-Fi Aware as a second transport | Faster and dialog-free, but many mid-range phones lack it, so it can't be the baseline. Prove BLE end to end first. | After 1.0 |
| Wi-Fi Direct | Dropped: system invitation prompt on connect, Location Mode needed even on Android 13+, one-owner group model unsuited to a churning mesh, device name on the air. | Not planned unless phone tests contradict the research |
| Text chat | Briar and others already do secure messaging well; chat multiplies bandwidth and the attack surface. Reports are the product. | Not planned; link to Briar instead |
| Photos, video, voice | Far beyond BLE bandwidth; photos carry metadata and faces that endanger people. | Not planned |
| Internet or satellite relays, bridges to other networks | Breaks "no servers, no internet"; a bridge is an observation point. | Not planned |
| iOS | Android only (constraint 1). Apple's restrictions on background radio would also limit relaying. | Not planned |
| App lock / duress PIN | Valuable against seized unlocked phones; needs threat-model input on what it really buys. | v1.1 **[DEP: threat]** |
| Removing one member from a group | Needs re-keying; in the MVP you make a new group and re-share in person. | v1.x **[DEP: threat]** |
| Free-text notes on reports | Free text is a channel for doxxing, spam and fingerprinting; fixed types first. | Later, if asked for |
| Custom report types | Same reason; a fixed catalog keeps reports tiny and comparable. | Later |
| Routing ("safe way out") | Needs reliable data the mesh can't guarantee; a wrong route is dangerous. | Not planned |
| LoRa / Meshtastic hardware bridge | Interesting for range; extra hardware and a new transport. | Later, as an adapter |
| Play Store | GitHub Releases first, F-Droid later. | Later |
| Translations | English only until the spec says otherwise. | Later (community) |

---

## 4. User stories

Format: *As a [persona], I want [action], so that [outcome].* IDs map to features.

### Getting ready (before the protest, usually with internet)
- **US-1 (F1)** As a marcher, I want the app to tell me plainly what it protects me from and what it can't, so that I don't take risks I wouldn't otherwise take.
- **US-2 (F2)** As a marcher, I want to download the map of my city once at home, so that the map works when the network is cut.
- **US-3 (F2)** As a marcher without a good connection, I want to import a map file a friend gave me, so that I never need the internet at all.
- **US-4 (F8)** As an affinity group member, I want to create a group and have my friends scan a QR code from my screen, so that only people I physically met can join.

### On the street
- **US-5 (F3)** As a marcher, I want to turn the mesh on with one tap and see how many phones are nearby, so that I know whether reports can reach me.
- **US-6 (F3)** As a marcher, I want the app to keep relaying while my phone is in my pocket, so that I help the network without holding my phone.
- **US-7 (F4)** As a marcher, I want to drop a "tear gas" report on the map in two taps, so that people nearby can avoid it.
- **US-8 (F4)** As a marcher, I want to place the report where the thing is, not where I am, so that my own position isn't broadcast.
- **US-9 (F5)** As a marcher, I want to see reports from people several hops away, so that I learn about things I can't see.
- **US-10 (F6)** As a marcher, I want to confirm a report I can see, so that others know it's still true.
- **US-11 (F6)** As a marcher, I want to dismiss a report that's no longer true, so that people aren't misled.
- **US-12 (F7)** As a marcher, I want old reports to vanish on their own, so that the map shows what's happening now.
- **US-13 (F9)** As an affinity group member, I want to send a report only to my group, so that strangers and police can't read it.
- **US-14 (F9)** As an affinity group member, I want to tell at a glance which reports come from my group, so that I trust them more than anonymous ones.
- **US-15 (F10)** As an affinity group member, I want to share my position with my group only, and stop with one tap, so that we can find each other.
- **US-16 (F13)** As a marcher, I want a discreet mode, so that a lit-up map doesn't draw attention.

### When things go wrong
- **US-17 (F11)** As a marcher about to be detained, I want to wipe the app in one deliberate move, so that a seized phone reveals nothing about my group.
- **US-18 (F12)** As a marcher whose phone was taken, I want the app to hold no history of where I was, so that it can't be used against me or others.
- **US-19 (F5, NFR)** As a marcher, I want the app to keep working when someone floods the mesh with junk, so that one attacker can't silence everyone.

### Contributor
- **US-20 (F14)** As a contributor, I want a documented transport contract and a loopback mesh of simulated phones, so that I can test a new radio or a relay change without a crowd.
- **US-21 (F14)** As a security reviewer, I want the threat model, protocol and wire format written down in one place, so that I can check the code against them.

---

## 5. Functional requirements

### F1 — Honest onboarding
- FR-1.1 First launch shows a short sequence (3–5 screens) that says what the app does and, in plain words, **what it protects** (message contents within a group, no server, no history) and **what it doesn't** (anyone nearby with a receiver can tell a phone is running the app; anyone in your group can read group messages and see shared positions; public reports can be fake; a phone seized unlocked before a wipe exposes current data). Meaning is fixed by the claims list in [threat-model.md §7](threat-model.md); wording is the design thread's. **[DEP: design]**
- FR-1.2 Permissions are asked one at a time, each with a one-sentence reason, only when the feature needs it. The exact set per Android version comes from the transport research. **[DEP: transport]**
- FR-1.3 The same "What this app protects" page is reachable from settings at any time.

### F2 — Offline map
- FR-2.1 The map renders entirely offline from OpenStreetMap-derived data with an open-source, GPL-3.0-compatible renderer that makes no network calls. **[DEP: stack]**
- FR-2.2 Getting map data is always an explicit user action. Two paths: (a) download a region, (b) import a map file through the Android file picker.
- FR-2.3 Preferred design for (a), to decide in an ADR: the app opens the download link in the user's browser and then imports the file, so the app itself needs **no `INTERNET` permission at all**. Fallback: an in-app download confined to one code path, enforced by the manifest gate. **[DEP: stack]**
- FR-2.4 Map files can be shared phone to phone the same way (Android share sheet / file), so one person with internet can equip a group.
- FR-2.5 The map shows the user's own position when location is on, from the platform `LocationManager` (no Play Services). The own position is never stored.
- FR-2.6 Manage maps: list, size, delete. Optional: include map files in the panic wipe (F11), off by default because maps are public data but reveal the city of interest.

### F3 — Mesh presence
- FR-3.1 A single, always-visible control turns the mesh on and off. Off by default on first launch.
- FR-3.2 While on, a status shows: peers in range (approximate count), time since last report received, and whether relaying is active. Never shows peer identities. **[DEP: design]**
- FR-3.3 While on, relaying continues with the screen off through one user-visible `connectedDevice` foreground service with a persistent notification (neutral text in discreet mode). The service starts only when the user turns the mesh on with the app visible, never at boot. Turning the mesh off stops all radio activity.
- FR-3.3a OEM battery managers (Samsung, Xiaomi, Huawei and others) can kill the service. Onboarding links per-manufacturer instructions, and the mesh status says when relaying stopped unexpectedly.
- FR-3.4 The app relies on the OS's Bluetooth/Wi-Fi address randomization and never defeats it. Any token the app itself puts in an advertisement rotates at least as often as the OS address, in lock-step by restarting the advertising set, at most every 10 minutes, and cannot be linked across rotations. The Bluetooth device name is never advertised (gated). See [threat model §5.1](threat-model.md).
- FR-3.4a Residual risk, stated in onboarding: the app's service UUID is constant, so anyone with a receiver can tell that a phone running the app is nearby, though not whose.
- FR-3.5 If the radio is off or a permission is missing, the status says which and offers the fix.

### F4 — Drop a report
- FR-4.1 Report types are a fixed catalog for the MVP, each with an icon, colour and default lifetime (proposed, to tune in field tests):

  | Type | Default lifetime |
  |---|---|
  | Police line | 20 min |
  | Kettle / blocked in | 20 min |
  | Tear gas | 15 min |
  | Arrests happening | 15 min |
  | Medic here | 60 min |
  | Water / supplies | 90 min |
  | Exit open | 15 min |
  | Street closed | 30 min |
  | All clear here | 15 min |

- FR-4.2 Dropping a report: tap "Report", pick a type, place the pin (defaults to the map centre, **not** the user's GPS position), confirm. Two taps when the pin is already where it should be.
- FR-4.3 The user chooses the audience: **Everyone nearby** (public, unverified) or **one of my groups** (encrypted, verified). Default is the last choice made, shown on the send button.
- FR-4.4 A report carries only: type, position (coarsened to a precision set by the protocol), creation time (coarsened), lifetime, hop count, and the authentication data the protocol requires. No device, user or app-instance identifier, no free text. **[DEP: threat]**
- FR-4.5 A user can withdraw their own report; withdrawal spreads like a report.
- FR-4.6 Rate limit on the sender's side (e.g. at most one report per type per minute) so a mistake can't spam the mesh.
- FR-4.7 A report waits 5 seconds on the phone after the user confirms, with a visible **Cancel**, before it is sent. Once it has left the phone it can only be withdrawn (FR-4.5), which spreads more slowly than the report did. Discreet mode keeps the delay but drops the countdown animation. **[DEP: design]**

### F5 — Report spread
- FR-5.1 Relaying is epidemic gossip with anti-entropy, not blind flooding: phones advertise a small digest of what they hold, and on connection exchange a set summary and pull only the reports they lack, newest and most severe first, within a per-connection byte budget.
- FR-5.2 A report's id is the hash of the canonical signed report, so it identifies the content, not the sender. De-duplication is on that id everywhere. Each report carries a signed absolute expiry (the authoritative limit) and an unsigned hop budget decremented on each relay (caps spread in space). Nothing on the relay path names a relaying phone.
- FR-5.3 **Verify, then store, then relay.** A phone parses with a strict size cap (no compression on the wire), checks version, type, expiry and signature, and only then stores or forwards. Anything malformed, expired, out of hop budget, over-sized or over a rate limit is dropped silently without affecting other peers. Rate limits are per connection and global (ephemeral tokens rotate), and storage is bounded with eviction by expiry, then fewest confirmations. **[DEP: threat]**
- FR-5.4 Group reports are relayed by everyone, including phones outside the group, without being readable by them, so groups benefit from the whole crowd. Group frames carry no group id on the air and have a fixed size, and non-members can check them before relaying without reading them ([threat model §5.3](threat-model.md)).
- FR-5.5 A report shows on the map with its type, age, time left, and how it arrived (direct / relayed), never who sent it.
- FR-5.6 Every report the phone holds is also available as a list ("Reports near you"), sorted by distance from the map centre or your position, with the same filter as the map (FR-9.3), the same verified and unverified styles (FR-9.2) and the same detail. It is the main path for TalkBack users (NFR-12) and can drop a report at the map centre without touching the map. **[DEP: design]**

### F6 — Confirm or dismiss
- FR-6.1 On a report's detail, the user can **Confirm** (still true) or **Dismiss** (no longer true). One vote per report per phone.
- FR-6.2 Votes spread like reports and are authenticated the same way. The detail shows counts, split into "from your group" and "from nearby".
- FR-6.3 A confirmation refreshes the report's time left, up to a hard maximum of twice the default lifetime from creation, so nothing lives forever.
- FR-6.4 A pin is shown as "probably gone" (faded), never removed, only if **(a)** a group member dismissed it and no group member confirmed it after that, **or (b)** public dismissals are at least 3 and at least twice the public confirmations, and no group member confirmed it. Nothing removes a pin before expiry except a valid withdrawal by its author ([threat model §5.7](threat-model.md)).
- FR-6.5 The UI never presents a public count as proof; copy says "N nearby phones confirmed", not "verified".

### F7 — Expiry
- FR-7.1 Every report and vote has an absolute expiry. At expiry it is dropped from memory on every phone that holds it and is never relayed again (reports are never on disk, FR-12.2).
- FR-7.2 Each pin shows time left at a glance (expiry indicator). **[DEP: design]**
- FR-7.3 Each report carries a signed absolute creation time, to the minute. Relative ages are not used, because a relay could reset them and keep a report alive forever. When a phone's clock clearly disagrees with what it receives, the app warns the user to fix the clock.

### F8 — Affinity groups
- FR-8.1 Create a group: give it a local name (never sent over the air in clear), pick a lifetime (default 24 hours, up to 7 days), and the app generates the group's keys.
- FR-8.2 Join in person: a member shows a QR code holding a one-time join key and token; the joiner scans it with the in-app scanner (open-source, no Play Services). The group keys then travel in a Noise handshake over one direct radio hop between the two phones, **never** through the mesh. A photographed QR is worth at most one join, and every join is visible to the group ([threat model §5.5](threat-model.md)).
- FR-8.3 Any member can show the QR to add someone, so a group can grow at the meeting point. The QR screen warns that anyone who scans it can read the group's messages and see shared positions.
- FR-8.4 Each member picks a nickname for the group, sent only inside the group's encryption. Default is a random friendly name, not the phone's owner name.
- FR-8.5 Leave a group: deletes its keys from the phone. Removing someone else is out of scope for the MVP: make a new group and re-share in person (stated in the UI).
- FR-8.6 A group expires at the end of its lifetime: keys are deleted on every member's phone.
- FR-8.7 A phone can be in several groups (MVP limit: 5); the group roster shows members seen recently and their last-seen age, nothing older. **[DEP: design]**

### F9 — Group reports
- FR-9.1 Reports and votes sent to a group are end-to-end encrypted to the group and authenticated as coming from a member. **[DEP: threat]**
- FR-9.2 Group reports are visually distinct (verified style, group colour) from public reports everywhere on the map and in lists. **[DEP: design]**
- FR-9.3 The map can filter: everything / my groups only.
- FR-9.4 **Mute this member**: a local action that hides a group member's reports, votes and position on this phone only, for an infiltrator or a member posting nonsense. It does not remove them from the group (FR-8.5).

### F10 — Position sharing
- FR-10.1 Off by default, every time. Turned on per group with an explicit action; turns itself off when the mesh is turned off, the app is killed, or after a chosen duration (default 2 hours).
- FR-10.2 Positions go only to that group, encrypted, at a modest interval (e.g. every 30–60 s, tuned to bandwidth **[DEP: transport]**), and each expires after a few minutes.
- FR-10.3 Received positions are kept in memory only, never written to disk; the map shows only the latest position per member with its age.
- FR-10.4 A persistent indicator shows while you are sharing, with one-tap stop.

### F11 — Panic wipe
- FR-11.1 A panic wipe control is reachable in at most two actions from any screen, and requires a deliberate gesture (e.g. hold for 2 seconds or slide) so it can't fire by accident in a pocket. **[DEP: design]**
- FR-11.2 The wipe destroys the storage encryption key first (making all stored data unreadable at once), then deletes data, groups, reports, settings and, if chosen, map files; then stops the mesh. Completes in under 2 seconds. **[DEP: threat]**
- FR-11.3 After a wipe the app looks like a fresh install.
- FR-11.4 A wipe sends nothing over the air (no "I wiped" signal that marks the person).

### F12 — Local protection
- FR-12.1 All app data is stored encrypted at rest with a key held in the Android Keystore. **[DEP: stack]** **[DEP: threat]**
- FR-12.2 Reports, votes and positions (your own and others') are held in memory only and never written to disk. What is stored (keys, groups, settings) is encrypted. So there is no location history, and a restart clears the map.
- FR-12.3 No logs with content, positions or keys in release builds; no crash reports.
- FR-12.4 Expired data is dropped, not just hidden; expired groups' keys are deleted from storage.
- FR-12.5 Screenshots of sensitive screens (QR join code) are blocked (`FLAG_SECURE`).

### F13 — Discreet mode
- FR-13.1 One switch: darkest theme, reduced brightness, no sounds or vibration, neutral notification text and icon.
- FR-13.2 Discreet mode does **not** disguise the launcher name or icon: a disguise found on a seized phone looks worse than the app itself (threat model recommendation).
- FR-13.3 A Quick Settings tile toggles discreet mode without opening the app. The user adds it; Echo never adds it on its own. The tile only toggles and shows on/off, never a report, a count or a group, so it reveals nothing on a locked phone beyond the app being installed.

### F14 — Transport architecture (product-level contract)
- FR-14.1 Core and UI depend only on a radio-agnostic `MeshTransport` contract in `:core:mesh`. They know capabilities (range class, bandwidth class, works in background, needs location permission, supports broadcast, max message size), never "BLE" or "Wi-Fi Direct". Same pattern as Zapper's ADR-0002. **[DEP: stack]**
- FR-14.2 Each transport is its own module `:adapters:<transport>` and registers with a registry. The MVP ships `:adapters:ble` only; extended BLE advertising is used as an optimisation when both phones support it, exposed as a capability (`maxBroadcastBytes`), never as the baseline.
- FR-14.3 `:adapters:loopback` simulates N phones in memory with configurable topology, latency, loss and hostile peers, so mesh, relay and crypto behaviour is tested on the JVM.
- FR-14.4 `docs/transports.md` explains how to add a transport.

### Settings screen (summary)
Mesh (on/off, background relaying), Groups (list, create, join, leave), Maps (download, import, delete), Privacy (discreet mode, position sharing default duration, include maps in wipe), Panic wipe, What this app protects, About (version, licence, source link, security contact, open-source licences).

---

## 6. Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-1 | Propagation | In a loopback simulation of 200 phones at crowd density, a report reaches 90% of phones within 5 hops in under 60 s. Field target on the reference phones: direct peer within 10 s. **[DEP: transport]** |
| NFR-2 | Message size | A signed report is compact (research estimate 110–150 bytes in a binary format, to confirm) and moves over a GATT/L2CAP connection; it never depends on fitting in a 31-byte legacy advertisement. Wire format in [ADR-0005](decisions/ADR-0005-wire-format.md) (proposed): a public report is 114 bytes. **[DEP: threat]** |
| NFR-3 | Robustness | A peer sending malformed, oversized or flooding traffic cannot crash the app or stop it relaying for other peers. The decoder is fuzzed in CI. |
| NFR-4 | Privacy on the air | No stable identifier on the air; app-level tokens rotate at most every 10 minutes, together with the radio address, and are unlinkable; no cleartext group names or nicknames. **[DEP: threat]** |
| NFR-5 | Privacy on the phone | No location history, no plaintext logs, encrypted storage, wipe under 2 s. |
| NFR-6 | Network | No traffic to anything but other phones running the app, over peer-to-peer radio. No `INTERNET` permission unless an ADR confines it to map download (FR-2.3); a gate enforces it. |
| NFR-7 | Dependencies | No Google Play Services, Firebase, closed SDK or tracker; all dependencies GPL-3.0-compatible; a gate fails the build on any forbidden artifact. |
| NFR-8 | Crypto | No home-made primitives or protocols; a well-reviewed library and a published design named in an ADR. Independent security review before 1.0. **[DEP: threat]** |
| NFR-9 | Battery | With the mesh on and the screen off, battery use stays under a target the transport research sets (indicatively < 5%/hour on the reference phones). **[DEP: transport]** |
| NFR-10 | Permissions | The "Nearby devices" permissions (`BLUETOOTH_SCAN` with `neverForLocation`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`), plus optional location only for showing your own position on the map. Never background location. Each requested in context (FR-1.2). |
| NFR-11 | Usability in the street | Usable one-handed and with gloves (touch targets ≥ 56 dp for primary actions), readable in sunlight, glanceable at arm's length; dark first. **[DEP: design]** |
| NFR-12 | Accessibility | Content descriptions everywhere, font scaling, WCAG AA contrast, TalkBack usable for drop/read report. |
| NFR-13 | Compatibility | Android 12 (API 31)+, see §10 item 11; portrait. Radio features (extended advertising, L2CAP, 2M/Coded PHY) are runtime capabilities and degrade gracefully when the chip lacks them. |
| NFR-14 | Honesty | Every safety claim in the UI traces to a row of [threat-model.md §7](threat-model.md); reviewed as part of the security review. |
| NFR-15 | Code quality | Mesh, relay and crypto logic are plain Kotlin/JVM, tested against simulated and hostile peers; CI on every PR. |
| NFR-16 | Distribution | Signed APK on GitHub Releases; reproducible builds suitable for F-Droid later. `SECURITY.md` with a private reporting route. |
| NFR-17 | Localization | All strings externalized. Superseded by NFR-21 in the [extended spec](SPEC-EXTENDED.md): English, French and Spanish at 1.0. |

---

## 7. Acceptance criteria (MVP definition of done)

Tested on the three reference phones (A, B and C below are any of them; every pair is tried at least once, including the OPPO), and on the loopback mesh where marked (L).

**Map and onboarding**
- [ ] AC-1 First launch shows the "what this app protects / doesn't" screens before any permission request.
- [ ] AC-2 A region downloaded or imported at home renders offline with flight mode on (radios for the mesh back on).
- [ ] AC-3 A map file shared from one phone and imported on the other renders identically.

**Reports between two phones**
- [ ] AC-4 With both phones in range and the mesh on, a public report dropped on phone A appears on phone B within 10 s, at the right place, with type and time left.
- [ ] AC-5 Phone B keeps receiving with its screen off and the app in the background.
- [ ] AC-6 A report disappears from both phones at expiry, and was never written to storage.
- [ ] AC-6a With A and C out of range of each other and B between them, a report from A reaches C through B (a real two-hop relay).
- [ ] AC-7 A confirmation from B shows on A within 10 s and extends time left, never beyond twice the default lifetime.
- [ ] AC-8 Dismissals mark a report "probably gone" without removing it.
- [ ] AC-9 (L) A report crosses 5 hops in a 200-phone simulation; 90% coverage in under 60 s.
- [ ] AC-10 (L) A report that was tampered with, replayed after expiry, or forged by a non-member as a group report is not shown and not relayed.
- [ ] AC-11 (L) A flooding peer at 100× normal rate does not stop honest reports from spreading to other peers.
- [ ] AC-12 The decoder fuzz target runs in CI with no crash.

**Groups**
- [ ] AC-13 A creates a group; B joins by scanning A's QR; a group report from A shows on B in the verified style.
- [ ] AC-14 (L) A phone outside the group relays the group report but cannot read it.
- [ ] AC-15 B shares position with the group; A sees it with its age; B stops with one tap and A's view goes stale then disappears.
- [ ] AC-16 After leaving or expiry, the group's keys are gone and its reports can't be read.

**Safety**
- [ ] AC-17 A panic wipe completes in under 2 s and leaves the app as a fresh install; nothing is sent over the air.
- [ ] AC-18 A file-level inspection of a debug copy of app storage after use shows no plaintext reports, positions, keys or group names.
- [ ] AC-19 A BLE capture over 30 minutes shows no app token or address that survives a 10-minute rotation.
- [ ] AC-24 Muting a group member hides their reports, votes and position on that phone within one refresh.
- [ ] AC-25 Scanning a QR a second time (a photographed code) fails to join, and the first join was shown to the group.

**Architecture and project**
- [ ] AC-20 No module in `app/` or `core/` imports transport adapter internals (`checkTransportBoundaries`).
- [ ] AC-21 The dependency gate fails on any Play Services, Firebase or tracker artifact; the manifest gate fails on an unapproved `INTERNET` permission.
- [ ] AC-22 CI builds, tests, lints, runs `rungs check` and produces a signed release APK on tag.
- [ ] AC-23 `docs/threat-model.md` covers every attack from both Bridgefy papers with a stated answer.

---

## 8. Proposed milestones

| Milestone | Content | Exit criteria |
|---|---|---|
| **M0 — Foundations** | Repo with rungs, CI and gates, module skeleton, `MeshTransport` contract, loopback adapter, design tokens, threat model v1, wire format ADR. | AC-20 → AC-22 (build part), AC-23 |
| **M1 — Two phones, one report** | First transport, signed expiring public reports, relay and de-dup, encrypted store, offline map. The basics. | AC-1 → AC-6a, AC-9 → AC-12 |
| **M2 — Groups** | QR join, encrypted group reports, verified style, confirm/dismiss, position sharing. | AC-7, AC-8, AC-13 → AC-16, AC-24, AC-25 |
| **M3 — Safety and street use** | Panic wipe, discreet mode, onboarding final copy, field test in a crowd. | AC-17 → AC-19 |
| **M4 — Release 1.0** | Independent security review and fixes, accessibility pass, docs, signed reproducible release. | All ACs |

A second transport (Wi-Fi Aware) comes after 1.0, unless the field test shows BLE can't meet NFR-1.

---

## 9. Assumptions and dependencies on sibling threads

| # | Assumption | Owner thread | Status |
|---|---|---|---|
| A1 | At least one peer-to-peer radio works on Android without Play Services, in the background, at crowd density, within the battery target. | Transport | ⚠️ Partly: BLE works on every target phone, needs no dialog and runs in a foreground service. Range in a crowd, battery, Doze and OEM battery managers must be measured on the reference phones (research §7). |
| A2 | A signed report fits the chosen transport's message budget (FR-4.4, NFR-2). | Transport + Threat | ✅ Over GATT/L2CAP, yes; not in a legacy advertisement, so reports move over connections (NFR-2). |
| A3 | A published protocol and a reviewed, GPL-compatible library cover signing, group encryption and QR key exchange. | Threat | ⚠️ Proposed in [ADR-0004](decisions/ADR-0004-crypto-design.md): Tink (Apache-2.0) for primitives, Noise for the join; awaiting acceptance. |
| A4 | Public reports can be authenticated without creating a linkable identity (rotating keys). | Threat | ⚠️ Proposed in [threat model §5.2](threat-model.md). |
| A5 | An open-source offline map renderer exists that is GPL-compatible and makes no network calls. | Stack | ⚠️ Proposed in [ADR-0006](decisions/ADR-0006-offline-map.md): VTM (LGPL-3.0), no network module, no permission; to measure on the reference phones. |
| A6 | Map data can be obtained without the app holding `INTERNET` (browser download + import). | Stack | ⚠️ Proposed in [ADR-0006](decisions/ADR-0006-offline-map.md): Mapsforge `.map` files downloaded in the browser and imported with the system file picker. |
| A7 | Stack is Kotlin + Compose, multi-module, as in Zapper's ADR-0001. | Stack | ⏳ Open (expected yes) |
| A8 | The design system delivers street-ready components for every screen above. | Design | ⏳ Open |
| A9 | `rungs` drives the repo workflow as in Zapper. | Stack | ⏳ Open (expected yes) |

## 10. Decisions log

1. ~~**License**~~ — **Decided:** GPL-3.0-only.
2. ~~**Platform and distribution**~~ — **Decided:** Android phones, GitHub Releases first, F-Droid later.
3. ~~**Language**~~ — **Decided:** English only for the MVP milestones; English, French and Spanish at 1.0 (Antoine, 2026-11-17, extended spec F18).
4. **App name** — **Decided by Antoine (2026-11-16):** Echo, package `io.github.throughthewind.echo`. Repo: `ThroughTheWind/echo-mesh`.
5. ~~**Reference phones**~~ — **Answered by Antoine (2026-11-16):** Pixel 11 Pro, Pixel 9a, OPPO Find X3 Pro. Android versions to record during the BLE spike.
6. ~~**Public reports to strangers**~~ — **Decided by Antoine (2026-11-16):** yes, shown as unverified and visually distinct from group reports (FR-4.3, FR-9.2).
7. **Default group lifetime** — *Proposed:* 24 h, up to 7 days (FR-8.1). Short-lived groups limit what an infiltrator or a seized phone exposes.
8. **No `INTERNET` permission** — *Proposed:* map download goes through the browser and is imported (FR-2.3). Proposed in [ADR-0006](decisions/ADR-0006-offline-map.md), awaiting acceptance.
9. **Background relaying** — *Proposed:* yes, through a visible foreground service while the mesh is on (FR-3.3).
10. ~~**Non-members relay group traffic**~~ — **Decided (threat model):** yes, as fixed-size frames with no group id on air (FR-5.4).
11. ~~**minSdk**~~ — **Decided by Antoine (2026-11-16):** 31 (Android 12+), so Bluetooth never requires a location permission. Phones stuck on Android 8–11 are not supported.
12. ~~**MVP transport**~~ — **Decided (research):** BLE only; Wi-Fi Aware later; Wi-Fi Direct dropped.
13. ~~**Send delay, report list, discreet tile**~~ — **Decided by Antoine (2026-11-17):** in the MVP, from the design system (FR-4.7, FR-5.6, FR-13.3).

---

## 11. Glossary

- **Mesh** — the set of phones running the app within radio range of each other, directly or through relays.
- **Transport / adapter** — one radio technology (BLE, Wi-Fi Direct…) behind the `MeshTransport` contract.
- **Capabilities** — what a transport declares it can do; drives behaviour and UI, never the radio's name.
- **Report** — a typed, map-pinned, expiring message. **Public** (everyone nearby, unverified) or **group** (encrypted, verified).
- **Vote** — a confirm or dismiss on a report.
- **Affinity group** — people who exchanged a group key in person by QR.
- **Hop** — one phone-to-phone relay step.
- **Lifetime / expiry** — how long a report lives; after it, every phone deletes it.
- **Loopback** — in-memory transport simulating many phones for tests.
- **Panic wipe** — immediate destruction of all app data on the phone.
