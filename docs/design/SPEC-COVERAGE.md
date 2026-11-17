# Spec coverage

Every requirement, user story and acceptance criterion in [`docs/SPEC.md`](../SPEC.md), and the design entries that answer it. Rows marked **No UI** have nothing to draw; they're listed so a missing row always means a gap, never an omission by choice.

`python3 -B tools/tokens.py check` fails when an id in the spec has no row here, or when a row names an id the spec no longer has. Add the row in the same change that adds the requirement.

## Functional requirements

| Id | Requirement | Design |
|---|---|---|
| FR-1.1 | Onboarding says what it protects and doesn't | [Onboarding](patterns/OnboardingFlow/README.md), [What Echo protects](screens/WhatEchoProtects/README.md), [Content and voice](foundations/ContentAndVoice/README.md) |
| FR-1.2 | Permissions one at a time, in context | [Permissions in context](patterns/PermissionsInContext/README.md) |
| FR-1.3 | Protects page reachable from settings | [What Echo protects](screens/WhatEchoProtects/README.md), [Settings](screens/SettingsScreen/README.md) |
| FR-2.1 | Offline map renderer | [Map view](screens/MapView/README.md) (map tokens in [Colour](foundations/Colour/README.md)) |
| FR-2.2 | Getting a map is an explicit action: download or import | [Get a map](patterns/GetAMap/README.md), [Maps](screens/MapsScreen/README.md) |
| FR-2.3 | Download through the browser, then import | [Get a map](patterns/GetAMap/README.md) |
| FR-2.4 | Share map files phone to phone | [Get a map](patterns/GetAMap/README.md), [Maps](screens/MapsScreen/README.md) |
| FR-2.5 | Own position, never stored | [Map view](screens/MapView/README.md), [Permissions in context](patterns/PermissionsInContext/README.md) |
| FR-2.6 | Manage maps; optionally wipe them | [Maps](screens/MapsScreen/README.md), [Panic wipe](patterns/PanicWipeConfirm/README.md) |
| FR-3.1 | One always-visible mesh switch, off at first launch | [Mesh status](components/MeshStatus/README.md), [Mesh on and off](patterns/MeshOnOff/README.md) |
| FR-3.2 | Peers, last report, relaying; no identities | [Mesh status](components/MeshStatus/README.md) |
| FR-3.3 | Foreground service with a persistent notification | [System notification](components/SystemNotification/README.md), [Mesh on and off](patterns/MeshOnOff/README.md) |
| FR-3.3a | OEM battery killers: instructions and "relaying stopped" | [Mesh on and off](patterns/MeshOnOff/README.md), [Mesh status](components/MeshStatus/README.md), [Permissions in context](patterns/PermissionsInContext/README.md) |
| FR-3.4 | Address randomization, rotating tokens | No UI: radio behaviour. Its residual risk is stated in [Onboarding](patterns/OnboardingFlow/README.md), [What Echo protects](screens/WhatEchoProtects/README.md) |
| FR-3.4a | Constant service UUID stated in onboarding | [Onboarding](patterns/OnboardingFlow/README.md), [What Echo protects](screens/WhatEchoProtects/README.md) |
| FR-3.5 | Status names a missing radio or permission and offers the fix | [Mesh status](components/MeshStatus/README.md), [Empty and error states](patterns/EmptyAndErrorStates/README.md), [Permissions in context](patterns/PermissionsInContext/README.md) |
| FR-4.1 | Fixed catalogue of nine report types | [Report pin](components/ReportPin/README.md), [Report picker](patterns/ReportPicker/README.md) |
| FR-4.2 | Report, pick, place at map centre, confirm | [Report picker](patterns/ReportPicker/README.md), [Map view](screens/MapView/README.md) |
| FR-4.3 | Audience: everyone nearby or one group | [Report picker](patterns/ReportPicker/README.md) |
| FR-4.4 | Minimal report contents, no free text | [Report picker](patterns/ReportPicker/README.md) |
| FR-4.5 | Withdraw your own report | [Report detail](patterns/ReportDetail/README.md) |
| FR-4.6 | Sender-side rate limit | [Report picker](patterns/ReportPicker/README.md) |
| FR-5.1 | Gossip with anti-entropy | No UI: transport. |
| FR-5.2 | Content-addressed ids, signed expiry, hop budget | No UI: protocol. |
| FR-5.3 | Verify, then store, then relay | No UI: protocol. Dropped frames are silent by design. |
| FR-5.4 | Everyone relays group reports unread | No UI: protocol. |
| FR-5.5 | Type, age, time left, direct or relayed; never the sender | [Report detail](patterns/ReportDetail/README.md), [Report pin](components/ReportPin/README.md), [Reports near you](screens/ReportsNearYou/README.md) |
| FR-6.1 | Confirm or dismiss, one vote | [Report detail](patterns/ReportDetail/README.md) |
| FR-6.2 | Counts split group and nearby | [Report detail](patterns/ReportDetail/README.md) |
| FR-6.3 | Confirmations extend time left, capped at 2x | [Expiry indicator](components/ExpiryIndicator/README.md), [Report detail](patterns/ReportDetail/README.md) |
| FR-6.4 | "Probably gone" rule, never removed | [Report pin](components/ReportPin/README.md), [Report detail](patterns/ReportDetail/README.md), [Reports near you](screens/ReportsNearYou/README.md) |
| FR-6.5 | A public count is never proof | [Report detail](patterns/ReportDetail/README.md), [Content and voice](foundations/ContentAndVoice/README.md) |
| FR-7.1 | Absolute expiry, dropped from memory | [Expiry indicator](components/ExpiryIndicator/README.md) |
| FR-7.2 | Time left at a glance on every pin | [Expiry indicator](components/ExpiryIndicator/README.md), [Report pin](components/ReportPin/README.md) |
| FR-7.3 | Signed creation time; warn when the clock is wrong | [Empty and error states](patterns/EmptyAndErrorStates/README.md) |
| FR-8.1 | Create a group: local name, lifetime | [Create a group](patterns/CreateGroup/README.md) |
| FR-8.2 | Join in person by one-time QR, direct handshake, join visible | [QR join flow](patterns/QrJoinFlow/README.md) |
| FR-8.3 | Any member can show the QR, with a warning | [QR join flow](patterns/QrJoinFlow/README.md), [Group roster](screens/GroupRoster/README.md) |
| FR-8.4 | Nickname per group, random friendly default | [Create a group](patterns/CreateGroup/README.md), [QR join flow](patterns/QrJoinFlow/README.md), [Group roster](screens/GroupRoster/README.md) |
| FR-8.5 | Leave a group; can't remove others (stated) | [Mute, leave and group end](patterns/MuteLeaveAndGroupEnd/README.md), [Group roster](screens/GroupRoster/README.md) |
| FR-8.6 | Group expires, keys deleted | [Mute, leave and group end](patterns/MuteLeaveAndGroupEnd/README.md) |
| FR-8.7 | Up to 5 groups; roster shows members seen recently | [Group roster](screens/GroupRoster/README.md) |
| FR-9.1 | Group reports encrypted and authenticated | No UI: crypto. Its visible result is the verified style in [Report pin](components/ReportPin/README.md) |
| FR-9.2 | Group reports visually distinct everywhere | [Report pin](components/ReportPin/README.md), [Report detail](patterns/ReportDetail/README.md), [Reports near you](screens/ReportsNearYou/README.md), [Colour](foundations/Colour/README.md) |
| FR-9.3 | Filter: everything or my groups only | [Map view](screens/MapView/README.md), [Chip](components/Chip/README.md) |
| FR-9.4 | Mute a member on this phone | [Mute, leave and group end](patterns/MuteLeaveAndGroupEnd/README.md), [Group roster](screens/GroupRoster/README.md) |
| FR-10.1 | Sharing off by default, per group, auto-off, default 2 h | [Position sharing](patterns/PositionSharing/README.md) |
| FR-10.2 | Positions only to the group, short-lived | [Position sharing](patterns/PositionSharing/README.md) |
| FR-10.3 | Latest position per member with its age, memory only | [Position sharing](patterns/PositionSharing/README.md), [Map view](screens/MapView/README.md), [Group roster](screens/GroupRoster/README.md) |
| FR-10.4 | Persistent sharing indicator with one-tap stop | [Position sharing](patterns/PositionSharing/README.md), [System notification](components/SystemNotification/README.md), [Map view](screens/MapView/README.md) |
| FR-11.1 | Wipe within two actions, deliberate gesture | [Panic wipe](patterns/PanicWipeConfirm/README.md), [Hold-to-confirm button](components/HoldToConfirmButton/README.md) |
| FR-11.2 | Key destroyed first, under 2 s | [Panic wipe](patterns/PanicWipeConfirm/README.md) (progress copy only; the mechanism has no UI) |
| FR-11.3 | After a wipe the app looks like a fresh install | [Panic wipe](patterns/PanicWipeConfirm/README.md), [Onboarding](patterns/OnboardingFlow/README.md) |
| FR-11.4 | A wipe sends nothing | [Panic wipe](patterns/PanicWipeConfirm/README.md) (stated in its copy) |
| FR-12.1 | Encrypted at rest with a Keystore key | No UI: storage. |
| FR-12.2 | Reports and positions in memory only; a restart clears the map | [Empty and error states](patterns/EmptyAndErrorStates/README.md), [What Echo protects](screens/WhatEchoProtects/README.md) |
| FR-12.3 | No content logs, no crash reports | No UI: build. |
| FR-12.4 | Expired data dropped, not hidden | No UI: storage. The visible side is [Expiry indicator](components/ExpiryIndicator/README.md) |
| FR-12.5 | QR screen blocks screenshots | [QR join flow](patterns/QrJoinFlow/README.md) |
| FR-13.1 | One switch: darkest theme, dim, silent, neutral notification | [Discreet mode](patterns/DiscreetMode/README.md), [Themes](foundations/Themes/README.md), [System notification](components/SystemNotification/README.md) |
| FR-13.2 | No launcher disguise | [Discreet mode](patterns/DiscreetMode/README.md) |
| FR-14.1 | Radio-agnostic transport contract | No UI: architecture. The UI shows capabilities only, see [Mesh status](components/MeshStatus/README.md) |
| FR-14.2 | One module per transport, BLE only in the MVP | No UI: architecture. |
| FR-14.3 | Loopback transport | No UI: test tooling. |
| FR-14.4 | How to add a transport | No UI: documentation. |

## Non-functional requirements

| Id | Requirement | Design |
|---|---|---|
| NFR-1 | Propagation targets | No UI: measured in simulation and the field. |
| NFR-2 | Compact signed report | No UI: wire format. |
| NFR-3 | Robust against hostile peers | No UI: decoder. |
| NFR-4 | No stable identifier on the air | No UI: radio. Stated in [What Echo protects](screens/WhatEchoProtects/README.md) |
| NFR-5 | No history, encrypted storage, fast wipe | [Panic wipe](patterns/PanicWipeConfirm/README.md), [What Echo protects](screens/WhatEchoProtects/README.md) |
| NFR-6 | No traffic but phones; no INTERNET permission | [Get a map](patterns/GetAMap/README.md) (the browser does the download) |
| NFR-7 | No proprietary dependency | [Iconography](foundations/Iconography/README.md), [Typography](foundations/Typography/README.md) (bundled, open-licensed icons and font) |
| NFR-8 | Reviewed crypto | No UI: crypto. |
| NFR-9 | Battery target | [Motion](foundations/Motion/README.md) (still map, no idle animation) |
| NFR-10 | Nearby devices, optional foreground location, never background | [Permissions in context](patterns/PermissionsInContext/README.md) |
| NFR-11 | One hand, gloves, sunlight, glance, dark first | [Principles](foundations/Principles/README.md), [Spacing and layout](foundations/SpacingAndLayout/README.md), [Themes](foundations/Themes/README.md), [Typography](foundations/Typography/README.md) |
| NFR-12 | Content descriptions, font scaling, contrast, TalkBack | [Accessibility](foundations/Accessibility/README.md), [Typography](foundations/Typography/README.md) |
| NFR-13 | Android 12+, portrait | [Spacing and layout](foundations/SpacingAndLayout/README.md) |
| NFR-14 | Every safety claim traces to threat model §7 | [What Echo protects](screens/WhatEchoProtects/README.md), [Onboarding](patterns/OnboardingFlow/README.md), [Content and voice](foundations/ContentAndVoice/README.md) |
| NFR-15 | Plain Kotlin logic, tested | No UI: code quality. |
| NFR-16 | Signed APK, reproducible builds | No UI: release. |
| NFR-17 | English only, strings externalized | [Content and voice](foundations/ContentAndVoice/README.md) |

## User stories

| Id | Requirement | Design |
|---|---|---|
| US-1 | Told plainly what it protects | [Onboarding](patterns/OnboardingFlow/README.md), [What Echo protects](screens/WhatEchoProtects/README.md) |
| US-2 | Download my city's map at home | [Get a map](patterns/GetAMap/README.md) |
| US-3 | Import a map file from a friend | [Get a map](patterns/GetAMap/README.md) |
| US-4 | Create a group, friends scan my QR | [Create a group](patterns/CreateGroup/README.md), [QR join flow](patterns/QrJoinFlow/README.md) |
| US-5 | Mesh on in one tap, see phones nearby | [Mesh on and off](patterns/MeshOnOff/README.md), [Mesh status](components/MeshStatus/README.md) |
| US-6 | Keep relaying in my pocket | [Mesh on and off](patterns/MeshOnOff/README.md), [System notification](components/SystemNotification/README.md) |
| US-7 | Tear gas report in two taps | [Report picker](patterns/ReportPicker/README.md) |
| US-8 | Place the report where the thing is | [Report picker](patterns/ReportPicker/README.md), [Map view](screens/MapView/README.md) |
| US-9 | See reports from several hops away | [Report detail](patterns/ReportDetail/README.md), [Reports near you](screens/ReportsNearYou/README.md) |
| US-10 | Confirm a report | [Report detail](patterns/ReportDetail/README.md) |
| US-11 | Dismiss a report | [Report detail](patterns/ReportDetail/README.md) |
| US-12 | Old reports vanish | [Expiry indicator](components/ExpiryIndicator/README.md) |
| US-13 | Send a report to my group only | [Report picker](patterns/ReportPicker/README.md) |
| US-14 | Tell my group's reports at a glance | [Report pin](components/ReportPin/README.md) |
| US-15 | Share position with my group, stop in one tap | [Position sharing](patterns/PositionSharing/README.md) |
| US-16 | Discreet mode | [Discreet mode](patterns/DiscreetMode/README.md) |
| US-17 | Wipe in one deliberate move | [Panic wipe](patterns/PanicWipeConfirm/README.md) |
| US-18 | No history of where I was | [What Echo protects](screens/WhatEchoProtects/README.md), [Empty and error states](patterns/EmptyAndErrorStates/README.md) |
| US-19 | Keeps working under flooding | No UI: protocol. |
| US-20 | Transport contract and loopback mesh | No UI: contributor tooling. |
| US-21 | Threat model and protocol in one place | No UI: documentation. |

## Acceptance criteria

| Id | Requirement | Design |
|---|---|---|
| AC-1 | Protects/doesn't screens before any permission | [Onboarding](patterns/OnboardingFlow/README.md), [Permissions in context](patterns/PermissionsInContext/README.md) |
| AC-2 | Downloaded or imported map renders offline | [Get a map](patterns/GetAMap/README.md), [Map view](screens/MapView/README.md) |
| AC-3 | Shared map file renders identically | [Get a map](patterns/GetAMap/README.md) |
| AC-4 | Report appears within 10 s with type and time left | [Report pin](components/ReportPin/README.md), [Expiry indicator](components/ExpiryIndicator/README.md) |
| AC-5 | Receives with the screen off | [Mesh on and off](patterns/MeshOnOff/README.md), [System notification](components/SystemNotification/README.md) |
| AC-6 | Disappears at expiry, never stored | [Expiry indicator](components/ExpiryIndicator/README.md) |
| AC-6a | Two-hop relay | [Report detail](patterns/ReportDetail/README.md) (shows "Relayed 2 hops") |
| AC-7 | Confirmation extends time left, capped | [Report detail](patterns/ReportDetail/README.md), [Expiry indicator](components/ExpiryIndicator/README.md) |
| AC-8 | Dismissals mark "probably gone", not removed | [Report pin](components/ReportPin/README.md), [Report detail](patterns/ReportDetail/README.md) |
| AC-9 | 5 hops in simulation | No UI: simulation. |
| AC-10 | Tampered or forged reports neither shown nor relayed | No UI: protocol. |
| AC-11 | Flooding peer doesn't stop spread | No UI: protocol. |
| AC-12 | Decoder fuzzing in CI | No UI: CI. |
| AC-13 | Create, join by QR, group report in verified style | [Create a group](patterns/CreateGroup/README.md), [QR join flow](patterns/QrJoinFlow/README.md), [Report pin](components/ReportPin/README.md) |
| AC-14 | Non-member relays but can't read | No UI: protocol. |
| AC-15 | Share position, age shown, stop, goes stale | [Position sharing](patterns/PositionSharing/README.md), [Group roster](screens/GroupRoster/README.md) |
| AC-16 | Keys gone after leaving or expiry | [Mute, leave and group end](patterns/MuteLeaveAndGroupEnd/README.md) |
| AC-17 | Wipe under 2 s, fresh install, nothing sent | [Panic wipe](patterns/PanicWipeConfirm/README.md) |
| AC-18 | No plaintext in storage | No UI: storage. |
| AC-19 | No token survives rotation | No UI: radio. |
| AC-20 | Transport boundaries gate | No UI: architecture. |
| AC-21 | Dependency and manifest gates | No UI: build. |
| AC-22 | CI builds and signs | No UI: CI. |
| AC-23 | Threat model covers Bridgefy attacks | No UI: documentation. |
| AC-24 | Muting hides a member within one refresh | [Mute, leave and group end](patterns/MuteLeaveAndGroupEnd/README.md), [Group roster](screens/GroupRoster/README.md) |
| AC-25 | A photographed QR fails; the first join was shown | [QR join flow](patterns/QrJoinFlow/README.md) |
