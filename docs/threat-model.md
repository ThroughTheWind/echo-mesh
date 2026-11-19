**Authoritative for:** who the app defends against, what each adversary can and cannot learn or do,
the security design that answers each, the Bridgefy attack checklist, the honest-claims list for the
UI, and what must be reviewed by an outside expert before 1.0.
**Not authoritative for:** the choice of crypto library and protocol, and why (see
[ADR-0004](decisions/ADR-0004-crypto-design.md)); product behaviour (see [SPEC.md](SPEC.md)); radio
details (see [research/](research/)).

# Threat model

| | |
|---|---|
| **Status** | v0.1 draft, 2026-11-16. Not reviewed by an outside expert. |
| **Applies to** | MVP (spec v0.1): public reports, votes, affinity groups, group reports, position sharing, panic wipe |
| **Design it pairs with** | [ADR-0004](decisions/ADR-0004-crypto-design.md) (accepted) |

> Everything in this document is a design target until a test or the outside review shows it holds.
> The UI may only claim what §7 lists as protected (spec NFR-14).

---

## 1. What we protect, in order

1. **People's safety and identity.** Who is at the protest, who is in which group, where a person
   is right now and where they were.
2. **Group confidentiality and authenticity.** Only members read group traffic; a group report
   really comes from a member, and from the member it says.
3. **The mesh keeps working.** No message, peer or flood can knock a phone off the mesh or hide a
   real danger.
4. **A seized phone reveals as little as possible**, and nothing about the past beyond the last few
   hours.

**Not protected, by design:** the content of public reports (they are public), and the fact that a
phone runs the app (see §7).

## 2. Assets

| Asset | Where it lives | Lifetime |
|---|---|---|
| Group secret (current epoch key only) | Encrypted at rest (§5.6), in memory while the mesh runs | ≤ group lifetime (24 h default, 7 days max) |
| Member signing key (Ed25519, one per group membership) | Same as above | Same as the group |
| Group roster: members' public signing keys and nicknames | Same as above | Same as the group |
| Reports, votes, withdrawals | **Memory only** (§5.6) | ≤ 2 × default lifetime (≤ 3 h) |
| Signing key of each public report the user sent | Memory only | Until the report expires (needed to withdraw it) |
| Members' positions | Memory only, latest per member | A few minutes |
| The user's own position | Never stored, never sent unless sharing is on | — |
| Offline map files | Plain files | Until deleted; optional in the wipe (spec FR-2.6) |

## 3. Adversaries

The brief names four. Two more are added because the design has to answer them anyway.

| # | Adversary | Capabilities assumed | Goals |
|---|---|---|---|
| **A1** | **State with radio sniffers** | Passive BLE and Wi-Fi capture anywhere in the crowd, many receivers, unlimited storage and compute, correlation with CCTV and cell data. Can also run the app (it is public) and act as any number of peers. | Count and locate app users, track a person over time and across days, map groups and who talks to whom, read traffic, find who originated a report. |
| **A2** | **Infiltrator inside a group** | Was shown the QR in person and joined legitimately. Holds the group key and a valid member signing key. May pass everything to A1. | Read the group, see shared positions, learn nicknames, inject false reports, impersonate other members, stay hidden. |
| **A3** | **Phone seizure** | Gets a phone: (a) locked, after first unlock (the usual case at an arrest), with forensic tools; (b) unlocked, or unlock coerced. Also combines it with A1's past captures. | Read groups, rosters, reports, positions; decrypt previously captured traffic; impersonate the user in their groups afterwards. |
| **A4** | **Flooder / saboteur** | Any number of phones or laptops running a modified app, inside or outside groups, can craft arbitrary bytes. | Inject fake public reports and votes, hide real dangers, exhaust battery and bandwidth, crash or stall phones (Bridgefy's "single carefully crafted message"). |
| **A5** | **Fake or tampered app** | Publishes a modified APK, or compromises a dependency. | Ship a trojaned build to protesters. |
| **A6** | **Network observer during map download** | Sees the one allowed internet use (spec F2). | Learn which city a user is preparing for. |

**Out of scope** (stated in the UI and SECURITY.md): an OS already compromised by spyware or a
malicious OS image; physical coercion of a member to reveal the group in person; radio jamming (no
software defence; the app should say "no peers" honestly, not crash); traffic analysis by a global
adversary that sees every radio at once is **reduced, not defeated** (§6).

## 4. What each adversary can and cannot do

### A1 — State with radio sniffers

| Can | Cannot (by design, to be verified) |
|---|---|
| Tell that a phone near it runs the app (its service advertisement is recognisable). Count app phones in an area. | Link a phone across the app's rotation period by anything the app puts on the air (§5.1). Link it across days. |
| Read every public report and vote, like anyone running the app. | Read group reports, positions, votes or roster changes. Tell group frames of one group from another's: there is no group identifier on the air (§5.3). |
| Guess who originated a report by being the first to hear it (first-hop inference), and approximate where it entered the mesh. | Learn it from the message: frames carry no sender, receiver, or relay path (no Bridgefy "track" field), only a hop count. |
| See that a phone sends group frames at a regular rhythm (looks like position sharing). | Tell report from position from vote by size: all group frames have one fixed size (§5.4). |
| Link a phone's scanning and its advertising by timing, within one rotation period (Android has no passive-scan switch, §5.1). | Carry that link past the next OS address rotation through anything the app sends. |
| Join the mesh as many peers (A4). | Learn whether two phones met before: every encounter runs the same exchange, with no cached peer state (Bridgefy P1 §4.1, P2 §4.5). |

### A2 — Infiltrator inside a group

| Can | Cannot |
|---|---|
| Read all group traffic from the epoch they joined until the group expires or is re-formed. Pass the key to A1, who then reads it too. | Read group traffic from before they joined (they receive only the current epoch key, §5.3). |
| See nicknames and the shared positions of members who share. | Join silently: joining needs a one-time code shown in person, and every join is announced to the group with the new nickname (§5.5). |
| Post false group reports and votes **under their own member key**. | Post as another member: every group message is signed by the sender's member key inside the encryption. |
| Admit others (any member can, spec FR-8.3), each admission signed by them. | Remove a member or rotate the key. Neither can anyone else in the MVP: re-form the group in person (spec FR-8.5). |

**Answer:** small groups, joins visible to all, attribution of everything a member sends, a local
**mute this member** action (recommended for the spec), position sharing off by default, short
group lifetimes, and honest copy at the QR screen.

### A3 — Phone seizure

| Case | Gets | Does not get |
|---|---|---|
| Locked (after first unlock), forensic extraction of storage | Map files (which city). That the app is installed. | Group keys, rosters, nicknames: they are encrypted with a Keystore key that requires the device to be unlocked (§5.6). Reports and positions: never written to disk. |
| Locked, memory extraction while the mesh runs (advanced) | Whatever is in memory: current group keys, live reports, latest positions. | Anything older than the current epochs. |
| Unlocked or coerced unlock, before a panic wipe | Current groups, rosters, nicknames, live reports and positions. Can read and post in the user's groups as the user until the groups expire or are re-formed. | Past group traffic captured by A1 before the retained epochs (forward secrecy by the hourly ratchet, §5.3). Location history: there is none. |
| After a panic wipe | A fresh install. | Anything: the storage key is destroyed first (crypto-erase), so leftovers on flash are unreadable. |

**Answer:** memory-only reports, Keystore key bound to an unlocked device, epoch ratchet, panic
wipe that crypto-erases, no logs. Members should treat a member's arrest as a group compromise and
re-form the group; the app should say so.

### A4 — Flooder / saboteur

| Can | Cannot |
|---|---|
| Inject any number of fake **public** reports and votes, each with a fresh key (Sybil). | Make them look like group reports: group reports need the group key and a member key. |
| Inject opaque junk that looks like group frames; non-members relay it (§5.3). | Get junk past members: it fails decryption and is dropped. Get unlimited relay: per-link rate limits and bounded queues apply to all frames. |
| Mass-dismiss a public report. | Hide it: dismissals only fade a pin, never remove it, and public votes alone cannot fade a report confirmed by a group member (§5.7). |
| Send malformed, oversized, or deeply nested input. | Crash or stall a phone: no compression, a size limit checked before parsing, a strict fixed-layout decoder that is fuzzed, and authentication before any expensive processing (§5.4). |
| Replay old frames. | Keep a report alive: lifetime is computed from the signed creation time, and relays cannot change it. |

**Answer:** the app makes public reports cheap to read and cheap to ignore. The UI labels them
unverified and lets the user filter to "my groups only" (spec FR-9.3). Proof-of-work on public
reports was considered and rejected for the MVP (it costs protesters' batteries more than an
adversary's laptop).

### A5 — Fake or tampered app

Answer: signed GitHub releases with the signing-certificate fingerprint published in the README and
SECURITY.md, reproducible builds for F-Droid, the dependency gate (no Play Services, Firebase or
trackers), dependency verification metadata, and a small dependency tree (crypto is one library,
ADR-0004).

### A6 — Network observer during map download

Answer: the download is opt-in and HTTPS; the UI says it reveals which region is downloaded to the
network and to the host; sideloading a map file is offered as the private alternative (spec F2).

## 5. Security design

The crypto design and the reasons for it are in [ADR-0004](decisions/ADR-0004-crypto-design.md).
This section states what each part must achieve.

### 5.1 Nothing stable on the air

- The app relies on the OS's Bluetooth and Wi-Fi address randomisation and never defeats it (no
  fixed address, no device name, no Bluetooth pairing).
- Any value the app itself puts on the air (an advertising payload, a rendezvous value, a session
  token) is random, rotates at most every **10 minutes**, and **rotates together with the radio
  address**: the adapter stops and restarts the advertising set so the old payload never appears
  with the new address. A payload that survives an address change links the two and defeats the
  OS's randomisation. **[DEP: transport]** confirms what each radio API allows.
- No "known peer" state: phones that met before run exactly the same exchange as strangers.
- The sync exchange (which messages do you have) happens inside a connection, never in an
  advertisement, and message ids in it are blinded with a per-connection random salt. The set of
  messages a phone holds is still a weak fingerprint across a short time; this is a known limit
  (§6).
- Checked against the Android radio APIs by the transport research
  ([peer-to-peer-transports.md](research/peer-to-peer-transports.md) §1.3, §2.1, §5.3). Residual
  risks until its tests 7 and 9 settle them:
  - **The app cannot read its own advertising address**, so "payload rotates with the address" is
    verified by a second phone capturing, not by the app. The OS may also rotate on its own timer
    (about 15 minutes); the test must show the two never drift apart.
  - **The phone's Bluetooth name** may be readable by a connected peer through Android's built-in
    Generic Access service, which the app cannot remove. Mitigation: links use L2CAP only, with no
    GATT server, and onboarding advises setting a generic Bluetooth name. Until test 9 shows the
    name is unreachable, the UI must not claim the name is hidden.
  - **Scanning as central** uses the system-wide private address on the OS's schedule, and the
    public API has no passive-scan switch. A sniffer may link a phone's scanning and advertising by
    timing within one rotation period (added to A1 below).
  - Salted inventories force blind connections to unfamiliar tokens; these are capped per minute
    (also a battery limit under A4).

### 5.2 Public reports and votes

- Each public report is signed with a **fresh Ed25519 key** made for that report only. Two reports
  from the same phone are unlinkable.
- The signature does not say who sent it. It does three things: relays cannot alter the report;
  the message id (hash of the signed bytes) is stable; and the author, who keeps the key in memory
  until expiry, can sign a **withdrawal** (spec FR-4.5).
- Public votes are signed the same way, one fresh key per vote. "One vote per phone" is enforced on
  the voting phone only; others cannot check it.
- The signed body contains the creation time (to the minute) and the lifetime. Receivers reject a
  report created more than 5 minutes in their future and drop it at creation + lifetime (+ 2 minutes
  of tolerated skew). This answers spec FR-7.3 differently from its suggestion: relative ages are
  rejected because a relay could reset them and keep a report alive forever. A phone warns that its
  clock is wrong when the signed creation times it receives disagree with it by more than 5
  minutes: "behind" when most recent reports are from its future, "ahead" when even the freshest
  quarter is older than that, since a phone receives old reports with new ones (`ClockCheck`).

### 5.3 Group traffic

- A group has a 256-bit group secret, made by the creator. Keys are derived per hourly **epoch**
  from it by a one-way ratchet: `K(n+1) = HKDF(K(n))`, with the epoch index taken from UTC time.
  A phone keeps only the epochs a live message can still use (≤ 4 previous, because the longest
  report life is 3 hours) and deletes older ones. A joiner gets only the current epoch key.
- A group frame is `XChaCha20-Poly1305(epoch message key, random 24-byte nonce, inner)`, where
  `inner = member key id ‖ payload ‖ Ed25519 signature by the member key over (group context ‖
  epoch ‖ payload)`. Sign, then encrypt: outsiders learn nothing about the sender; members learn
  exactly who sent it.
- **No group identifier on the air.** A receiver tries each of its groups' current keys (at most 5
  groups × a few epochs); a failed AEAD check is cheap. This avoids Bridgefy's lesson that a shared
  visible key or tag lets outsiders map traffic.
- **Non-members relay group frames** (answers spec FR-5.4): groups need the crowd's reach, and the
  fixed frame size plus the missing group id keep what a relay learns to "a group frame passed
  here". Non-members cannot authenticate them, so they are relayed under the same rate limits as
  everything else.
- Payload types inside a group frame: report, vote, withdrawal, position, member-added, nickname.

### 5.4 Wire format and decoder

- **No compression, anywhere.** Both Bridgefy papers trace attacks to it: a decompression bomb
  (P1 §4.4, P2 §4.3), a padding oracle hidden in gzip fields (P1 §4.3), and plaintext recovery
  from compressed lengths (P2 §6).
- Every frame starts with a version byte; unknown versions are dropped. Length is checked against
  the transport's maximum **before** parsing. The layout is fixed, with no nesting, no optional
  fields, no trailing bytes accepted. Signatures cover the exact received bytes, never a
  re-encoding.
- All group frames are padded to one fixed size, so type is not visible from length.
- Order of checks on receipt: size → version → expiry → de-duplication → signature or AEAD →
  semantic validation → store and relay. Relays forward only after verifying (Bridgefy forwarded
  before processing, P1 §4.4).
- The decoder is fuzzed in CI (spec NFR, brief). Every wire change bumps the version and adds fuzz
  seeds. [ADR-0005](decisions/ADR-0005-wire-format.md) fixes the format against these
  constraints. Mechanical enforcement: CI fuzzes the decoder with a planted-bug check, nothing in
  `core/` may import or depend on a compression library (`checkCompressionImports`,
  `gradle/forbidden-in-core.txt`), only `:core:crypto` may import a crypto API
  (`checkCryptoBoundaries`), the dependency gate admits Tink but rejects Play Services and
  Firebase, and release builds strip `android.util.Log`.
- Error handling is uniform: every rejected frame is dropped silently, after the same work as far
  as practical, and never answered. No delivery receipts exist, so there is no oracle (Bridgefy's
  Bleichenbacher attack used receipts, P1 §4.3).

### 5.5 Joining a group in person

- A member's phone shows a QR holding a fresh, **one-time** join key (X25519 public key), a random
  128-bit token, and a random rendezvous value, valid for 2 minutes or one join. Only 8 bytes or
  fewer derived from the rendezvous value are advertised (a BLE advert has no room for more next to
  the service UUID); a collision only costs a failed handshake, because the PSK authenticates.
- The joiner scans it, finds the member's phone over a direct radio link (one hop, never relayed
  through the mesh), and runs the Noise handshake `Noise_NKpsk0_25519_ChaChaPoly_SHA256` with the
  join key as the responder's static key and the token as the PSK.
- Inside it, the joiner sends a new member signing public key and nickname; the member sends the
  current epoch key, the group's lifetime, and the roster. The member's phone then posts a signed
  **member-added** message to the group, so everyone sees the newcomer.
- Why not just put the group key in the QR: a photo of that QR (over a shoulder, by a camera, or a
  screenshot) would give silent, permanent read access to the whole group. With this design a photo
  is worth at most one join, which takes the slot of the real joiner: the real joiner's join fails
  and the member sees a nickname they don't recognise. Both are visible.

### 5.6 Storage, memory and wipe

- Reports, votes, positions and the keys of one's own public reports live **in memory only**. If
  the app is killed they are lost and peers resend them. This removes a whole class of seizure risk
  and of deletion bugs (spec FR-12.4).
- What is stored (groups, roster, epoch key, member key, settings) is encrypted with a key from the
  Android Keystore, hardware-backed (StrongBox where present), created with
  `setUnlockedDeviceRequired(true)` (available on every supported version, since minSdk is 31 per
  ADR-0001).
- **Panic wipe:** delete the Keystore key first (crypto-erase), clear memory, then delete files,
  then stop the radio. Sends nothing.
- No logs with content, positions, keys, or peer data in release builds (enforced by a lint rule
  or by R8 stripping `android.util.Log`, **[DEP: stack]**). `FLAG_SECURE` on screens that show
  keys, QR codes or the map with positions.

### 5.7 Votes and the "probably gone" rule (answers spec FR-6.4)

- Group votes count once per member key. Public votes count once per signature key, which a
  flooder can mint freely, so they are shown as "N nearby phones", never as proof.
- A pin fades to "probably gone" only if **(a)** a group member dismissed it and no group member
  confirmed it after that dismissal, **or (b)** public dismissals are at least 3 and at least twice
  the public confirmations, and no group member has confirmed it. Nothing ever removes a pin before
  its expiry except a valid withdrawal by its author.

### 5.8 Relay budget (fake public reports and relay abuse)

Public reports are in the MVP (spec §10 item 6), so A4 can always inject fakes. The design limits
what fakes cost everyone else:

- **Per-link limits:** each connection has a frame-rate and byte budget, and a budget for
  signature checks per second. Over budget, frames from that link are dropped, not queued. Limits
  are per link, not per sender, because senders have no identity.
- **Bounded memory:** the store of live public reports has a fixed cap (a few hundred). When full,
  it evicts from the link that brought the most frames, oldest first, so a flooder pushes out its
  own fakes rather than everyone else's reports; a phone's own reports are never evicted. Group
  reports and reports confirmed by a group member are never evicted by public ones (Phase 2).
- **Summaries** (spec FR-5.1, [ADR-0008](decisions/ADR-0008-gossip-summary-frame.md)): a phone
  answers a neighbour's summary at most once per half period per link, within a byte budget, so an
  empty summary from a hostile peer costs it a bounded amount.
- **Relay priority:** group frames first, then public reports confirmed by a group member, then
  other public reports by age. A flood of fakes slows other public reports, never group traffic.
- **Sender-side limit** (spec FR-4.6) protects against mistakes only; a modified app ignores it.
- **Density check:** if one map cell receives more than a set number of public reports of one type
  within a few minutes, they are shown as one cluster ("12 nearby reports"), so a flood cannot
  cover the map. Thresholds are tuned in field tests.
- **Honest UI:** public reports are styled as unverified everywhere and the map can show "my groups
  only". The app never turns public numbers into a verdict.

## 6. Bridgefy attack checklist

Sources:
- **P1** — M. R. Albrecht, J. Blasco, R. B. Jensen, L. Mareková, "Mesh Messaging in Large-scale
  Protests: Breaking Bridgefy", CT-RSA 2021. <https://eprint.iacr.org/2021/214>
- **P2** — M. R. Albrecht, R. Eikenberg, K. G. Paterson, "Breaking Bridgefy, again: Adopting
  libsignal is not enough", USENIX Security 2022.
  <https://www.usenix.org/conference/usenixsecurity22/presentation/albrecht>

Each row becomes a test (loopback mesh with hostile peers, or a radio capture on the reference
phones) before 1.0. "Test" names what proves the defence holds.

| # | Attack (paper, section) | Root cause in Bridgefy | Our defence | Test |
|---|---|---|---|---|
| 1 | Local tracking via advertisements (P1 §4.1) | Stable user-id checksum in BLE advertisement | §5.1: nothing stable on the air, payload rotates with the address | Spec AC-19 radio capture |
| 2 | Historical contact tracing from handshake shape (P1 §4.1, P2 §4.5) | Cached keys make known pairs' handshakes shorter | §5.1: no peer state; every encounter identical | Loopback: captured exchange for met / never-met pairs is byte-length identical |
| 3 | Participant identification by contact discovery (P1 §4.1) | Phone numbers as identity | No accounts, no phone numbers, no contacts permission | Manifest gate: no `READ_CONTACTS` |
| 4 | Social graph from plaintext sender/receiver ids (P1 §4.1) and from a shared key plus relay "track" field (P2 §4.4) | Metadata in clear or under a public key | No sender, receiver, group id or path on the air; only a hop count | Frame-layout review; decoder test that rejects any extra field |
| 5 | Impersonation by unauthenticated, stateless handshake (P1 §4.2) | Ids and addresses treated as credentials | No identity at the link layer; group authenticity comes from member signatures inside the group AEAD | Loopback: hostile peer reusing another's key id is rejected |
| 6 | Spoofing mesh messages (P1 §4.2) | Sender id is a plaintext claim | Every frame is signed (public) or AEAD + signed (group) | Loopback: altered frame dropped, not relayed |
| 7 | Remote change of contact names (P1 §4.2) | Profile updates accepted over the mesh unauthenticated | Nicknames change only via signed group messages from the same member key | Loopback |
| 8 | MITM by key replacement / TOFU (P1 §4.3, P2 §4.1) | Keys fetched over the mesh, trust on first use, nothing to verify | Group keys are never sent through the mesh; join is authenticated by the QR's one-time key and token (§5.5); no 1:1 chat in the MVP | Loopback: join without the token fails; join relayed beyond one hop is refused |
| 9 | IND-CPA failure from deterministic RSA-ECB (P1 §4.3) | Textbook-style RSA PKCS#1 v1.5 per block | Only AEAD with random 192-bit nonces; no RSA | Code review |
| 10 | Files sent in plaintext (P1 §4.3) | Payload field outside the encryption | No attachments in the MVP; every group payload is inside the AEAD | Frame-layout review |
| 11 | Padding oracle through gzip and delivery receipts (P1 §4.3) | Unauthenticated ciphertext, compression, receipts | AEAD verified before anything else; no compression; no receipts or replies to rejected frames | Loopback: no observable reaction to invalid frames |
| 12 | Timing oracle on error type (P1 §4.3, sketch) | Different error paths take different time | Rejections are silent; AEAD check first; no reply exists to time | Code review |
| 13 | Decompression bomb takes down the mesh (P1 §4.4; reduced in P2 §4.3) | Unbounded gzip; relays forward before processing | No compression; size checked first; verify before relay; fuzzed decoder | Fuzz in CI; loopback with hostile peer sending max-size garbage at max rate |
| 14 | Targeted DoS between two users (P1 §4.4) | Attacker key bound to a user id | No user ids to bind to | Loopback |
| 15 | TOCTOU session hijack breaks 1:1 confidentiality (P2 §5) | Sessions looked up by a reassignable user id | No 1:1 sessions in the MVP; group keys are bound to the group, not to a peer | — (revisit if 1:1 chat is added) |
| 16 | Broadcast impersonation (P2 §4.2) | Broadcasts unauthenticated | Public: signed, and the UI never shows an author; group: member-signed | Loopback |
| 17 | Broadcast IND-CPA via compression before encryption (P2 §6.1) | Compress-then-encrypt | No compression | Code review + gate (no compression library on the classpath of `:core:*`) |
| 18 | Plaintext recovery from compressed lengths (P2 §6.2–6.5) | Length depends on content | No compression; group frames padded to one size | Loopback: all group frames equal length |

**The lessons, not just the attacks.** P2 (§7) warns about "'bolting on' security to an otherwise
unchanged design" and that "don't roll your own crypto" is "insufficient". So the security design
is written before the mesh code, and the outside review (§8) covers how the library is used, not
only which one.

**Open problem both papers leave open, and we do too:** P2 (§7) asks "how (flooding based) mesh
networking can efficiently provide sender and receiver privacy even against passive adversaries".
Our partial answers are no identifiers in frames, a random delay before a phone first sends its own
report (so originating looks like relaying), and fixed-size group frames. A dense sniffer network can
still estimate where a report entered the mesh.

## 7. What the app may claim (honest copy)

Every safety sentence in the UI traces to a row here (spec NFR-14). Final wording is the design
thread's; meaning is fixed here. Each row has an id (P for protects, N for does not protect) that the
app's strings cite; `scripts/gates/claims.mjs` fails when a claim cites no row, a row that does not
exist, or when a row is missing from the app's "What Echo protects" page.

**It protects**
- **P1** Group reports, positions and nicknames can be read only by people who joined the group in person.
- **P2** A group report shows which member sent it, and nobody can fake another member's report.
- **P3** Nothing you send carries your name, number or a fixed ID, and the app's radio signals change
  regularly.
- **P4** The app keeps no history of where you or others were. Reports disappear when they expire.
- **P5** A panic wipe makes everything the app stored unreadable within 2 seconds.
- **P6** There is no server, no account, and no internet use except a map download you start yourself.

**It does not protect**
- **N1** Anyone nearby with a radio receiver can tell your phone is running this app.
- **N2** Public reports can be fake, and their confirmations can be faked in bulk.
- **N3** Anyone in your group can read everything sent to it, see shared positions, and pass them on. If a
  member's phone is taken, assume the group is exposed and make a new one in person.
- **N4** If your phone is taken while unlocked, whoever has it can read your groups and post as you until
  you have wiped it or your group makes a new one.
- **N5** A determined observer with many receivers may guess roughly where a report was first sent.
- **N6** Downloading a map reveals the region to your network and the map host.

## 8. Before 1.0: outside review

An independent review by a cryptographer and a mobile-security reviewer, results published. Scope:

1. The Noise `NKpsk0` handshake implementation in `:core:crypto` against the Noise spec revision 34
   and published test vectors (cacophony).
2. The group frame construction: sign-then-encrypt, trial decryption, nonce generation, padding.
3. The epoch ratchet and its clock handling (skew, epoch boundaries, retention).
4. The one-time QR join: token entropy, expiry, race and replay behaviour, the one-hop rule.
5. The wire decoder and its fuzz harness coverage; the order of checks on receipt (§5.4).
6. Radio identifier rotation on the reference phones (captures, per transport adapter).
7. Keystore usage, memory-only data, and panic-wipe effectiveness on the reference phones.
8. The honest-claims list (§7) against the shipped UI copy.
9. The resolved dependency tree, release signing, and build reproducibility.

The review is cheaper and better once 1–7 have their own tests. Funding is open (for example,
programmes that fund audits of open-source tools for at-risk users); ask Antoine before contacting
anyone.

## 9. Open questions

None of these blocks Phase 1 or Phase 2: 1 is for v1.1, 2 and 3 are settled in the spec, and 4 and
5 are tuned by the field test (WI-028).

1. **App lock and duress PIN** (spec v1.1). An app lock that defers to the Android device
   credential adds little over the lock screen. A duress PIN that wipes is useful but can be read as
   destroying evidence in some jurisdictions, and its existence is discoverable in the open-source
   code. Proposal: v1.1, opt-in, with that warning. Needs Antoine's call.
2. **Launcher disguise** (spec FR-13.2). Proposal: no disguise. The app is still visible in system
   settings and the open-source code documents the disguise, so a disguised protest app that is
   found looks worse than an honest one. Discreet mode covers what is on screen. *Settled: spec
   FR-13.2.*
3. **Mute a member** locally (A2). Proposal: add to the spec as a small MVP item. *Settled: spec
   FR-9.4, built in WI-021.*
4. **Join over one hop needs a working radio link** at join time. If field tests show joins failing
   often, the fallback is a two-way QR exchange (both phones scan each other), which needs no radio.
5. **Rotation period** of 10 minutes and **epoch length** of 1 hour are first guesses; tune with the
   transport research and field tests.
