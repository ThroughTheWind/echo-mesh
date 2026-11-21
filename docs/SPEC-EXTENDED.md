# Echo — Extended Product Spec (1.0 additions and beyond)

| | |
|---|---|
| **Status** | v1.0 — all decisions taken (§11), from the accepted [assessment](https://claude.ai/artifact/XtQFJvB89nwgisZCJAAmhD) |
| **Extends** | [SPEC.md](SPEC.md) (MVP, F1–F14, AC-1–AC-25). That spec stays authoritative for everything it defines; this one adds to it and lists the MVP requirements it changes (§6). |
| **Date** | 2026-11-17 |
| **Releases covered** | 1.0 additions, 1.1, 2.0, later (evidence-gated) |
| **Numbering** | Continues the MVP: features F15+, user stories US-22+, NFR-18+, acceptance criteria AC-26+ |

Every feature here keeps the MVP's rules: fixed typed messages and no free text on the air, verify then
store then relay, nothing on disk a seizure could use, in-person trust only, and every UI claim traced to
a row of [threat-model.md §7](threat-model.md). A feature that needs a new threat-model section or ADR
says so with **[DEP: threat]** or **[DEP: ADR]**, and it does not start before that lands.

---

## 1. Vision for a mature Echo

The MVP proves one loop: drop a report, spread it, expire it. A mature Echo covers the whole day of a
protest, from preparation to jail support:

- **Before:** groups and organisations prepare a plan everyone carries (event packs), and the app itself
  can be passed phone to phone when the internet is already cut.
- **During:** reports, plus trusted roles (marshals, medics, legal observers, press), short group
  messages, danger alerts in the pocket, and announcements from organisers.
- **After:** check-ins that show who hasn't reported safe, and a jail-support card prepared in advance.

Echo stays **protest-first, crisis-compatible** (§11 item 1): the protest threat model remains the design
target, and crisis use (disasters, outages) comes only through report catalogs, never by loosening a
default.

---

## 2. New personas

| Persona | Description | Primary need |
|---|---|---|
| **Marshal** | Member of a union or organiser's stewarding team (*service d'ordre*). | Be trusted by strangers when saying "exit open" or "move back". |
| **Medic / legal observer** | Volunteer from an association. | Be findable, and have their reports trusted, for the duration of the event only. |
| **Organiser** | Prepares the route, meeting points and stations; coordinates on the day. | Distribute a plan offline and make rare, trusted announcements. |
| **Press** | Journalist accredited by a press organisation. | Be recognisable to protesters, knowing the exposure it brings. |
| **Crisis user** | Family or neighbour during a disaster or outage. | See where water, shelter and blocked roads are, without a network. |
| **Translator** | Contributor adding a language. | A glossary, a review checklist for the safety copy, and a test that nothing is missing. |

---

## 3. Scope

| # | Feature | Release | From the assessment |
|---|---|---|---|
| F15 | Steering reports with stronger warnings | 1.0 | R1 |
| F16 | Low-density honesty and carry-and-deliver | 1.0 | G2 |
| F17 | Field diagnostics file | 1.0 | G5 |
| F18 | Languages: English, French, Spanish, extensible | 1.0 | G7, R9 |
| F19 | Release key custody and security process | 1.0 | G6 |
| F20 | Canned group messages | 1.1 | E4 |
| F21 | Group rally point | 1.1 | E6 |
| F22 | Nearby danger alerts | 1.1 | E5 |
| F23 | Check-ins and jail-support card | 1.1 | G4 |
| F24 | App lock, duress PIN and auto-wipe | 1.1 | G3 |
| F25 | Short-lived encrypted report cache | 1.1 | R2 |
| F26 | Adaptive scanning | 1.1 | R7 |
| F27 | Handoff to Briar | 1.1 | E10 |
| F28 | Organisation keys and trust store | 2.0 | E1–E3 prerequisite |
| F29 | Roles: marshal, medic, legal observer, press, organiser | 2.0 | E2 |
| F30 | Organiser announcements | 2.0 | E3 |
| F31 | Event packs | 2.0 | E1 |
| F32 | Signed report catalogs, including a crisis catalog | 2.0 | R3, E8 |
| F33 | Wi-Fi Aware transport for bulk transfer | 2.0 | R6 |
| F34 | Share Echo offline, with authenticity check | 2.0 | G1 |
| F35 | Circles: long-lived groups that spawn event groups | Later | R4 |
| F36 | Group management at scale | Later | G8 |
| F37 | Areas to avoid | Later | E7 |
| F38 | LoRa / Meshtastic bridge adapter | Later | E9 |
| F39 | Legacy build for Android 8–11 | Later, only if field data shows the need | R5 |

Process item, not a feature: **R8**, re-deciding the vote thresholds, rotation period and epoch length
with field data before 1.0 (§9, M3).

### 3.1 Still out of scope

Unchanged from the assessment's "advised against" list, and recorded here so it isn't reopened without
a reason: free-text chat (F27 hands off to Briar instead), photos, video and evidence capture, internet
bridges and cloud backup, reputation scores for strangers, any map of where Echo users are, routing or
"safe way out", launcher disguise, proof-of-work, iOS until the Android version is audited, and any
automatic "I've been arrested" broadcast.

---

## 4. User stories

### 1.0 additions
- **US-22 (F15)** As a marcher, I want a public "exit open" from strangers to look clearly unconfirmed, so that a fake one can't lead me into a kettle.
- **US-23 (F16)** As a marcher, I want the app to tell me when almost nobody nearby runs Echo, so that an empty map doesn't read as "all clear".
- **US-24 (F17)** As a field tester, I want to export a file of how the mesh behaved, without positions or reports, so that the team can tune the app with real data.
- **US-25 (F18)** As a French- or Spanish-speaking marcher, I want the app and its safety warnings in my language, so that I understand exactly what it protects.
- **US-26 (F19)** As a user installing an update, I want to know who signs Echo and how to check it, so that a coerced or fake update can be spotted.

### 1.1
- **US-27 (F20)** As a group member, I want to send "regroup at the pin" or "I'm leaving" in two taps, so that we coordinate without typing.
- **US-28 (F21)** As a group member, I want one agreed meeting point on the map, so that we know where to go if separated.
- **US-29 (F22)** As a marcher, I want my phone to vibrate in my pocket when tear gas is reported near me, so that I don't have to watch the screen.
- **US-30 (F23)** As a group member, I want to see who hasn't checked in by the agreed time, so that jail support starts early.
- **US-31 (F23)** As a marcher, I want my lawyer's number and a rights summary on my phone without internet, so that I have them if I'm detained.
- **US-32 (F24)** As a marcher, I want the app locked if my phone is taken unlocked, so that my groups aren't exposed in seconds.
- **US-33 (F25)** As a marcher on a phone that kills background apps, I want the map to survive a restart, so that I don't lose my picture of the street.
- **US-34 (F26)** As a marcher at a six-hour protest, I want the app to save battery when the crowd is dense, so that my phone lasts the day.
- **US-35 (F27)** As a group member, I want to set up a Briar contact with someone in person, so that we can chat securely when we need to.

### 2.0
- **US-36 (F28)** As a marcher, I want to choose which unions, associations or media I trust, so that nobody decides that for me.
- **US-37 (F29)** As a marcher, I want to recognise reports from marshals and medics of organisations I trust, so that I can act on them faster than on strangers.
- **US-38 (F29)** As a medic, I want to show a medic station that people can find, for this event only, so that I'm not identifiable afterwards.
- **US-39 (F30)** As an organiser, I want to tell the whole march "moving to point B", so that thousands of people can follow without a network.
- **US-40 (F31)** As an organiser, I want to give everyone the route, meeting points and stations before the day, so that the plan works even with nobody nearby.
- **US-41 (F32)** As a crisis user, I want report types for shelter, water and blocked roads, so that Echo helps after a disaster too.
- **US-42 (F34)** As a marcher, I want to pass Echo to a friend's phone during a shutdown and check it's the real app, so that the network can grow on the day.

### Later
- **US-43 (F35)** As a long-standing affinity group, I want to start an event group without meeting everyone again, so that our weekly actions are practical.
- **US-44 (F36)** As a group member, I want to remove someone from the group, so that a compromised member loses access.
- **US-45 (F38)** As a marshal with a LoRa radio, I want to link two distant parts of the march, so that reports cross the gap.

---

## 5. Functional requirements

### F15 — Steering reports (1.0)
- FR-15.1 The catalog marks **steering types**: report types that tell people where to go or that a place is safe. In the MVP catalog: Exit open, All clear here, Street closed.
- FR-15.2 Public steering reports stay public (decided). From strangers they show a distinct warning style ("Unconfirmed — could be a trap") on the pin, in the list and in the detail. **[DEP: design]**
- FR-15.3 For public steering reports, the public confirmation count is **not shown**. Only confirmations from group members (and, from 2.0, from trusted roles, F29) appear.
- FR-15.4 The "probably gone" rule (FR-6.4) is unchanged.
- FR-15.5 The warning copy traces to a new row of threat model §7. **[DEP: threat]**

### F16 — Low-density honesty and carry-and-deliver (1.0)
- FR-16.1 The mesh status shows a **low-density state** when fewer than a set number of distinct peer tokens were seen in the last few minutes (initial value: under 3 in 5 minutes, tuned in field tests). Copy: the map may be missing reports; an empty map does not mean all clear.
- FR-16.2 With zero peers, the map says so plainly instead of looking like a quiet street.
- FR-16.3 **Carry-and-deliver** is stated as a behaviour, not a mode: a phone keeps every live report it holds and offers it to each new peer until expiry (FR-5.1 already does this). Onboarding explains that walking with the mesh on helps reports cross gaps.
- FR-16.4 The density threshold and the copy are covered by the field-test protocol (F17).

### F17 — Field diagnostics file (1.0)
- FR-17.1 Off by default. When the user turns it on, the app counts, in memory: distinct peer tokens per minute, reports received and relayed per minute, delivery delay from creation time, connection failures by kind, battery level per 10 minutes, and whether the foreground service was killed. Every count is timestamped in minutes since the session started, never with wall-clock times or a date ([threat model §10](threat-model.md)).
- FR-17.2 It never records positions, report contents or types, map areas, group names, nicknames, keys or tokens.
- FR-17.3 The user exports it by hand as a file through the share sheet, after a screen that shows the whole file in plain text. Nothing is ever sent automatically.
- FR-17.4 The counters are wiped by the panic wipe and when diagnostics are turned off. They are never written to disk until the user exports.
- FR-17.5 `docs/field-testing.md` gives the field-test protocol: phone placement, density levels, what to measure, and how to submit a file.

### F18 — Languages (1.0)
- FR-18.1 Echo ships in **English, French and Spanish** at 1.0 (decided). The system language picks the default, and there is an in-app override.
- FR-18.2 **Extensible:** adding a language means adding a strings file plus a glossary entry, following `docs/translating.md`. No code change is needed.
- FR-18.3 Every string in the safety set (onboarding, "What this app protects", wipe, QR warning, steering warnings, role and announcement copy) is reviewed by a fluent speaker against threat model §7 before the language ships. Community drafts are welcome, but they don't ship unreviewed.
- FR-18.4 Layouts use start/end, not left/right, so a right-to-left language can be added later without rework. No right-to-left language ships at 1.0.
- FR-18.5 Report type labels always ship with their icon, so a report reads without words.

### F19 — Release key custody and security process (1.0)
- FR-19.1 Antoine holds the release signing key (decided). The key is stored offline with an encrypted offline backup, and the procedure is written in `docs/releasing.md`. A second copy sits in the repository's encrypted Actions secrets so the release workflow can sign (decided by Antoine, 2026-11-19); SECURITY.md states what that exposes.
- FR-19.2 The signing certificate's SHA-256 fingerprint is published in the README, `SECURITY.md` and every release note, and shown in the app's About screen as text and as a QR code.
- FR-19.3 `SECURITY.md` names a private reporting route, a response target, and how fixes are disclosed.
- FR-19.4 Every release is built reproducibly, and the steps to verify it independently are documented.
- FR-19.5 Known risk, stated in `SECURITY.md`: with one key holder, coercing or compromising that person could produce a malicious update. Before 2.0, revisit whether to add a second holder or co-signing (§11 item 4).

### F20 — Canned group messages (1.1)
- FR-20.1 Inside a group, a member can send one of a fixed set of phrases: "Regroup at the rally point", "I'm leaving", "I'm safe", "Need help here", "Medic needed here", "Moving now", "Wait for me". Phrases that name a place carry a pin.
- FR-20.2 They travel as a new payload type inside the existing group frame (threat model §5.3), with the same size, encryption and expiry (default 15 minutes). **[DEP: threat]**
- FR-20.3 There is no free text. The phrase set is part of the catalog (F32), so a translation changes the label, not the meaning.
- FR-20.4 They appear in a group feed and, for those with a pin, on the map. Like reports, they are memory-only.

### F21 — Group rally point (1.1)
- FR-21.1 Each group has at most one rally point: a pin plus an optional time, set by any member before or during the event.
- FR-21.2 The latest setting wins, and the group sees who set it. It lives as long as the group, is held in memory, and is re-shared by members to new joiners.
- FR-21.3 It is drawn distinctly on the map, and "Regroup at the rally point" (F20) points to it.

### F22 — Nearby danger alerts (1.1)
- FR-22.1 Opt-in. When a new report of a chosen type (default: tear gas, kettle, arrests happening, police line) lands within a chosen radius (default 200 m) of the user's position, the phone vibrates in a distinct pattern.
- FR-22.2 By default only group reports, and from 2.0 trusted role reports, trigger alerts. Public reports can be added by the user, with a warning about fakes.
- FR-22.3 A lock-screen alert shows no text, no type and no map: only a neutral notification that opens the app. Discreet mode keeps the vibration and nothing else.
- FR-22.4 Rate-limited: at most one alert per type per 2 minutes.
- FR-22.5 Needs the location permission while the mesh is on, in the foreground service only. Without it, alerts are off and the setting says why.

### F23 — Check-ins and jail-support card (1.1)
- FR-23.1 A member can start a **check-in** for one group: an interval (default 30 minutes) and an end time. The phone reminds them with a vibration, and one tap sends "I'm OK" (an F20 phrase).
- FR-23.2 When a member's check-in is overdue, the group sees "<nickname> missed a check-in at 16:30" in the roster and feed. Nothing is broadcast on the missing member's behalf: silence is the signal (FR-11.4).
- FR-23.3 The copy says that a missed check-in can just mean an empty battery or no peers nearby.
- FR-23.4 **Jail-support card:** the user writes a lawyer or legal-team number and notes, and can add a rights summary per country from the event pack (F31) or bundled text. It is stored encrypted on the phone, **never sent over the air**, and removed by the panic wipe. It opens in two taps from the app or its notification, but only after the device is unlocked, never on the lock screen.
- FR-23.5 Ending a check-in sends "I'm safe" to the group.

### F24 — App lock, duress PIN and auto-wipe (1.1)
- FR-24.1 **App lock** (opt-in): the app locks after a chosen time in the background (default 1 minute) and opens with a separate app PIN or the device credential. The mesh keeps relaying while it is locked.
- FR-24.2 **Duress PIN** (opt-in): a second PIN that opens a fresh-looking app after a silent panic wipe (F11). The setup screen warns that in some jurisdictions this can be treated as destroying evidence, and that its existence is documented in the open-source code. **[DEP: threat]**
- FR-24.3 **Auto-wipe** (opt-in): if the app hasn't been unlocked for a chosen time (default 24 hours), a background job runs the panic wipe.
- FR-24.4 All three are off by default and described honestly in "What this app protects".
- FR-24.5 The app PIN and the duress PIN have a hard limit on wrong attempts, with an optional panic wipe when it is reached.
- FR-24.6 Copy never claims that the app lock stops forensic tools: it protects against someone handed an unlocked phone, not against extraction ([threat model §11](threat-model.md)).

### F25 — Short-lived encrypted report cache (1.1)
- FR-25.1 Live reports, votes, rally points and group phrases may be cached on disk. A key that requires an unlocked device (`setUnlockedDeviceRequired`) can't encrypt while the phone is locked, so each entry is wrapped to an hourly Keystore public key, and only the matching private key, usable only while unlocked, can open it ([threat model §11](threat-model.md)).
- FR-25.1a After a kill while the phone is locked, the cache and the group keys load only at the next unlock. Until then the app relays but can't show or read group content, and the mesh status says so.
- FR-25.2 Each entry is crypto-erased at its expiry: each entry or batch has its own key, and that key is deleted. Received positions are **never** cached.
- FR-25.3 After a restart or a service kill, the map comes back with everything still live.
- FR-25.4 The panic wipe destroys the cache key first.
- FR-25.5 This changes FR-12.2 and threat model A3 (a locked phone holds live reports again, unreadable without unlock). It ships only after the threat model is updated and reviewed. **[DEP: threat]**

### F26 — Adaptive scanning (1.1)
- FR-26.1 The BLE adapter adjusts its scan and advertise duty cycle to density and change. It scans less when many peers are present and the digests haven't changed, and more when the area is sparse or new reports appear.
- FR-26.2 The policy is in `:core:mesh`, so it stays radio-agnostic, and is tuned in the loopback simulator and in field tests.
- FR-26.3 Target: at least 30% less battery per hour than a fixed duty cycle at high density, with no more than 25% longer median delivery delay. Both are measured with F17.

### F27 — Handoff to Briar (1.1)
- FR-27.1 From a group member's roster entry, "Chat in Briar" explains Briar in one line and, if Briar is installed, opens its add-contact flow so both people scan each other in person.
- FR-27.2 Echo sends nothing to Briar and stores nothing about the link. If Briar isn't installed, it links to Briar's F-Droid page, which the user opens in their browser.

### F28 — Organisation keys and trust store (2.0)
- FR-28.1 An **organisation** is a long-term Ed25519 root key, plus a display name and the roles it may issue, signed by that key. The root key stays offline with the organisation, ideally split between 2 or more people, and only signs issuer keys.
- FR-28.2 An **issuer key** is held by whoever issues roles on the day, is signed by the root, and is valid for at most 7 days.
- FR-28.3 Each user has a **trust store**: the organisations they chose to trust. An organisation is added only in person by QR, or by accepting it from an event pack (F31) after seeing its name and fingerprint. There is no built-in list and no Echo-run authority, and trust is never transitive.
- FR-28.4 The trust store is stored encrypted and removed by the panic wipe. Users can remove an organisation at any time.
- FR-28.5 **Revocation:** an issuer or root can sign a revocation of an issuer key or credential. Revocations spread through the mesh like reports and are carried in packs. Expiry remains the main protection.
- FR-28.6 New threat-model section (organisation compromise, stolen issuer key, coerced issuer, impersonated organisation) and a new ADR for the credential format. Outside review is required. **[DEP: threat]** **[DEP: ADR]**

### F29 — Roles (2.0)
- FR-29.1 Roles: **marshal, medic, legal observer, press, organiser**. An issuer grants a role in person by scanning the holder's QR. The **credential** contains the organisation fingerprint, role, validity window (24 hours at most), the holder's per-event public key, and the issuer's signature.
- FR-29.2 The holder's reports, votes and role pins are signed with the per-event key and carry the credential. To someone who trusts the organisation, they show a role badge and the issuer's name ("Marshal · issued by <name>"). To everyone else, they are ordinary public reports.
- FR-29.2a The holder chooses, per report, whether to sign it as their role or send it as an ordinary public report. The default is the role for the role's own types (station pins, steering reports), and ordinary for everything else.
- FR-29.3 **Stated cost:** a role holder's reports are linkable to each other for the credential's lifetime. The issuance screen says so, and press roles carry an extra warning about targeting.
- FR-29.4 Role pins: a medic or legal observer can show a **station** pin (lifetime up to the catalog's maximum, refreshed while they stay), so people can find them.
- FR-29.5 Trusted role confirmations count alongside group confirmations in FR-6.4 and FR-15.3.
- FR-29.6 Order of delivery: marshal and medic first, then legal observer and organiser, and press last.

### F30 — Organiser announcements (2.0)
- FR-30.1 A holder of the **organiser** role can send an announcement: a phrase from a fixed set ("March moving to pin", "Disperse now by pin", "Hold position", "Event over") with an optional pin.
- FR-30.2 It is shown as a banner to people who trust the organisation, with the issuer's name and fingerprint always visible. To others, it is not shown.
- FR-30.3 Each issuer can send at most 1 announcement per 5 minutes, and each lasts at most 30 minutes. Relays enforce both.
- FR-30.4 A newer announcement from the same organisation replaces the banner. Announcements from different organisations are shown separately and never merged.

### F31 — Event packs (2.0)
- FR-31.1 An **event pack** is a signed file containing: name, validity window, area, route, typed points (meeting, dispersal, medic station, water, toilets, legal point), a catalog reference (F32), a legal and rights text per country, and optionally organisation keys to offer (F28).
- FR-31.2 It is signed by a trusted organisation's issuer key, or by a group (shown only to that group). Group-signed packs ship only after the re-wrap design in [threat model §12.3](threat-model.md) is reviewed. **[DEP: threat]**
- FR-31.3 It is shared by file through the share sheet, over Wi-Fi Aware (F33), or over BLE when it is small enough. Size cap: 256 KB. It is never fetched from the internet by the app.
- FR-31.4 On import, the user sees the signer and fingerprint and chooses to accept. Organisation keys inside need a separate, explicit acceptance (FR-28.3).
- FR-31.5 The pack shows as a map layer that works with zero peers. It expires at the end of its validity window, can be removed, and is removed by the panic wipe.
- FR-31.6 New ADR for the pack format, versioned and fuzzed like the wire format. **[DEP: ADR]**

### F32 — Signed report catalogs, including crisis (2.0)
- FR-32.1 A **catalog** defines report types: id, icon from the bundled icon set, colour, default and maximum lifetime, steering flag (F15), alert eligibility (F22), and labels per language. The canned phrases (F20) and announcement phrases (F30) are part of the catalog too.
- FR-32.2 Echo bundles two catalogs, **Protest** (the default, extending the MVP set with water cannon, drones, plainclothes police, counter-protesters, charging point and toilets) and **Crisis** (shelter, water, food, injured, road blocked, power, charging point). Event packs may reference an organisation-signed catalog.
- FR-32.3 Hard caps hold whatever a catalog says: Protest lifetimes at most 3 hours (matching the threat model's epoch retention); Crisis lifetimes at most 12 hours. The 12-hour key retention is a per-group setting chosen when the group is created (a **crisis group**). Protest groups keep 3 hours and can't be switched later ([threat model §12](threat-model.md)).
- FR-32.4 Catalogs never add free text, names, photos or history, crisis included.
- FR-32.5 A report carries a catalog id and type id. A phone that lacks the catalog shows it as a generic "unknown type" pin and still relays it.
- FR-32.6 Onboarding offers the crisis catalog as a choice, with copy that doesn't present Echo's protest features as hidden.

### F33 — Wi-Fi Aware transport (2.0)
- FR-33.1 `:adapters:wifiaware` implements `MeshTransport` with a **bulk transfer** capability, used for app sharing (F34), maps (FR-2.4) and packs (F31), and opportunistically for report sync. It is gated at runtime on device support.
- FR-33.2 It follows the same rules as BLE: our own tokens rotate with the OS's NAN address and are unlinkable, and nothing is relayed unverified.
- FR-33.3 The research's permission and Location Mode caveats are stated in context when the user first starts a bulk transfer.
- FR-33.4 New ADR, plus identifier-rotation captures on the reference phones that support it. **[DEP: ADR]**

### F34 — Share Echo offline (2.0)
- FR-34.1 "Share Echo" sends the app's own installed APK through the share sheet or over Wi-Fi Aware.
- FR-34.2 The sender's screen shows the signing fingerprint (FR-19.2) as a short code and a QR. The receiver is guided to install it, then compares the code on first launch, where Echo shows its own fingerprint before anything else. The copy asks them to compare it with an **independent source** (a flyer, a trusted organisation, another phone's About screen), not only the sender's screen, since a fake app would show a matching fake code.
- FR-34.3 Copy states that Android refuses to update an installed Echo with an APK signed by a different key, so a fake can only reach phones without Echo.
- FR-34.4 Threat-model update for a fake or trojaned app spread this way (A5). **[DEP: threat]**

### F35–F39 — Later (evidence-gated)
- **F35 Circles.** A long-lived group secret, re-keyed on a schedule, from which members derive short-lived event groups without meeting again. Needs a ratchet design and outside review. Starts only if field use shows the 7-day group limit is pushing users to other tools. **[DEP: threat]** **[DEP: ADR]**
- **F36 Group management at scale.** Remove a member by re-keying, chained QR joins for 20 or more people, and links between groups. **[DEP: threat]**
- **F37 Areas to avoid.** Map shading from recent group and trusted-role reports only, never public ones, and explicitly "not a route".
- **F38 LoRa / Meshtastic bridge.** An adapter for external LoRa hardware with a long-range, tiny-bandwidth capability, carrying reports only. Needs its own identifier analysis. **[DEP: ADR]**
- **F39 Legacy build.** A separate build for Android 8–11 that asks for location with an explicit warning. Only if F17 data shows a significant share of users excluded.

---

## 6. Changes to MVP requirements

| MVP item | Change | Introduced by |
|---|---|---|
| FR-6.4, FR-6.5 | Trusted role confirmations count with group ones; public counts hidden for steering types | F15, F29 |
| FR-12.2 | Reports may be cached encrypted on disk while unexpired; positions still never | F25 (1.1) |
| FR-4.1 | Report types come from a catalog; the MVP list becomes the start of the Protest catalog | F32 (2.0) |
| FR-8.1 | Default lifetime unchanged; circles may later derive event groups | F35 (later) |
| §3.2 "App lock / duress PIN" | Specified | F24 (1.1) |
| §3.2 "Translations" | English, French, Spanish at 1.0 | F18 |
| NFR-17 | Replaced by NFR-21 | F18 |
| NFR-9 | Tightened by NFR-22 at high density | F26 |
| §8 M4 exit | Also requires AC-26 to AC-31 | 1.0 additions |

---

## 7. Non-functional requirements (added)

| ID | Category | Requirement |
|---|---|---|
| NFR-18 | Alerts | A danger alert (F22) fires within 5 s of the report being received. |
| NFR-19 | Diagnostics privacy | A test asserts that the diagnostics export (F17) contains only the fields of FR-17.1. |
| NFR-20 | Credentials | Relays and receivers reject role credentials and issuer keys outside their validity window, over the caps of FR-28.2 and FR-29.1, or revoked. Fuzzed like the wire decoder. |
| NFR-21 | Localization | English, French and Spanish complete. A CI gate fails on any missing string in a shipped language, and a pseudo-locale run catches truncation and hard-coded text. |
| NFR-22 | Battery | With F26, at most 3% per hour at high density, screen off, on the reference phones (measured, adjusted with F17 data). |
| NFR-23 | Packs and catalogs | Pack and catalog decoders have the same guarantees as NFR-3: size cap before parsing, signature before processing, fuzzed in CI. |
| NFR-24 | Release integrity | Every release's reproducibility is verified by a second build before publishing; the fingerprint is the same in README, `SECURITY.md`, release notes and the app. |

---

## 8. Acceptance criteria (added)

**1.0 additions**
- [ ] AC-26 A public "Exit open" from a stranger shows the steering warning and no public confirmation count; a group member's confirmation shows.
- [ ] AC-27 With fewer than the threshold of peers, the mesh status shows the low-density state; with none, the map says so.
- [ ] AC-28 A diagnostics export from a field session contains only FR-17.1 fields, and nothing is written to disk before export.
- [ ] AC-29 The app runs fully in French and Spanish with no missing or truncated strings; the safety set has a recorded fluent review.
- [ ] AC-30 The fingerprint shown in About matches the README, `SECURITY.md` and the release notes.
- [ ] AC-31 An independent rebuild of a release produces the same APK.

**1.1**
- [ ] AC-32 A canned phrase sent by A appears in B's group feed within 10 s; a non-member relays it and can't read it.
- [ ] AC-33 Setting a rally point on A shows it on B and C; a new joiner receives it.
- [ ] AC-34 With alerts on, a group "tear gas" report within 200 m vibrates B's locked phone with no text on the lock screen.
- [ ] AC-35 A missed check-in appears on the other members' phones; nothing is sent by the missing member's phone.
- [ ] AC-36 The jail-support card opens offline in two taps and is gone after a panic wipe.
- [ ] AC-37 With app lock on, the app asks for the PIN after the background timeout; the duress PIN opens a fresh app and leaves no readable data.
- [ ] AC-38 After the OS kills Echo, reopening it after unlock shows all reports still live; entries cached while locked are readable only after unlock; expired entries are unreadable on disk.
- [ ] AC-39 Adaptive scanning meets NFR-22 and FR-26.3 in the loopback simulator and on the reference phones.

**2.0**
- [ ] AC-40 After A trusts an organisation by QR, a marshal's report shows the badge and issuer name on A, and an ordinary public report on B (who doesn't trust it).
- [ ] AC-41 A credential past 24 hours, or revoked, is rejected and its reports shown as plain public.
- [ ] AC-42 An announcement shows as a banner to trusting phones; a second one within 5 minutes from the same issuer is dropped by relays.
- [ ] AC-43 An event pack imported from a file shows its layer with zero peers, asks separately before adding its organisation keys, and disappears at expiry.
- [ ] AC-44 A crisis-catalog report shows correctly on a phone with that catalog, and as "unknown type" (still relayed) on one without.
- [ ] AC-45 A 50 MB map file transfers between two Wi-Fi Aware phones; BLE-only phones are unaffected.
- [ ] AC-46 An APK shared from A installs on a phone without Echo and shows the same fingerprint; installing it over a differently signed Echo fails.

---

## 9. Releases

| Release | Content | Gate |
|---|---|---|
| **1.0** (MVP + additions) | MVP M0–M4, plus F15–F19, plus R8 re-tuning from field data | All MVP ACs, AC-26 to AC-31, independent security review |
| **1.1** | F20–F27 | AC-32 to AC-39; threat-model updates for F20, F24, F25 reviewed |
| **2.0** | F28–F34, with one combined crypto design for organisations, roles, announcements and packs | AC-40 to AC-46; outside review of the organisation layer and Wi-Fi Aware identifiers |
| **Later** | F35–F39, each started only when its evidence condition is met | Its own ACs, written when it starts |

---

## 10. Dependencies

| # | Needed before | What | Owner | Status |
|---|---|---|---|---|
| D1 | 1.0 | Threat model §7 rows for steering warnings and low density | Threat model | ✅ Drafted in threat model v0.2 (§7, §10) |
| D2 | 1.0 | `docs/translating.md`, glossary, French and Spanish reviewers | Antoine (reviewers), Claude (process) | ⏳ Open |
| D3 | 1.0 | Field-test protocol `docs/field-testing.md` | Claude | ⏳ Open |
| D4 | 1.1 | Threat model: group phrase payload, duress PIN, auto-wipe, report cache (changes A3) | Threat model | ✅ Drafted in threat model v0.2 (§11), ADR-0004 amended |
| D5 | 2.0 | ADR and threat-model section: organisation keys, issuers, credentials, revocation | Threat model | ⚠️ Drafted: threat model §12 and an organisation credentials ADR (proposed, number assigned when it lands, since ADR-0005 is the wire format); work item WI-053 |
| D6 | 2.0 | ADRs: event pack format, catalog format, Wi-Fi Aware transport | Stack | ⏳ Open |
| D7 | 2.0 | Threat model: crisis lifetimes up to 12 h, fake-app spread via sharing | Threat model | ✅ Drafted in threat model v0.2 (§12); group-signed packs await review of §12.3 |
| D8 | 2.0 | Funding for the second outside review | Antoine | ⏳ Open |

---

## 11. Decisions log

1. ~~**Protest tool or crisis tool**~~ — **Decided by Antoine (2026-11-17):** protest-first, crisis-compatible. Crisis use only through the Crisis catalog (F32); nothing that needs identity, history or free text. Reasoning and sources: assessment, "Answers to your two open questions".
2. ~~**Organisation keys**~~ — **Decided by Antoine (2026-11-17):** yes in 2.0, under the constraints of F28 and F29 (user-chosen trust, offline root, credentials of 24 hours or less, optional roles with stated cost, issuer always shown). Marshals and medics first, press last.
3. ~~**Languages**~~ — **Decided by Antoine (2026-11-17):** English, French, Spanish, easily extensible (F18).
4. ~~**Release signing key**~~ — **Decided by Antoine (2026-11-17):** Antoine holds it for now. Revisit a second holder before 2.0 (FR-19.5).
5. ~~**Steering reports public**~~ — **Decided by Antoine (2026-11-17):** they stay public, with the stronger warning and no public count (F15).
6. ~~**Everything else in the assessment**~~ — **Accepted by Antoine (2026-11-17):** gaps G1–G8, revisions R1–R9, extensions E1–E10, and the advised-against list.

---

## 12. Glossary (added terms)

- **Steering type** — a report type that tells people where to go or that a place is safe.
- **Low-density state** — mesh status shown when too few peers are nearby for the map to be trusted as complete.
- **Canned phrase** — a fixed group message from the catalog; no free text.
- **Rally point** — a group's single agreed meeting pin.
- **Check-in** — a timed "I'm OK" a member sends to their group; a missed one is visible to the group.
- **Organisation** — a union, association or media outlet with a root key that users may choose to trust.
- **Issuer key** — a short-lived key, signed by an organisation's root, used to grant roles on the day.
- **Role credential** — a signed statement that a per-event key holds a role for at most 24 hours.
- **Trust store** — the organisations a user has chosen to trust.
- **Event pack** — a signed, expiring bundle of route, points, catalog and legal text for one event.
- **Catalog** — the signed set of report types, phrases and lifetimes; Protest and Crisis are bundled.
