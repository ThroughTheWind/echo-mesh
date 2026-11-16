# QrJoinFlow

Join a group in person: a member shows a code, you scan it, you both compare four words, you pick a nickname (SPEC F8, FR-8.2, FR-8.3, FR-12.5). Keys never travel through the mesh.

**Compose:** `ShowCodeScreen`, `ScanCodeScreen`, `CompareWordsScreen`, `JoinedScreen`.

## Steps

1. **Show** (existing member): warning callout first, on `blocked-container`: "Show this only face to face, to people you trust. Anyone who scans it can read Crew's reports and see shared positions." Then the code: black on pure white in **every** theme (including discreet: scanners need it), 248dp, `radius-lg` white margin. Window brightness goes to full while it shows, `FLAG_SECURE` blocks screenshots, and the code changes on a timer if the protocol supports it ("new code in 1:48"). Under it, the four check words.
2. **Scan** (joiner): in-app viewfinder (open-source scanner, no Play Services), camera permission asked here and only here. "The camera is used only for this and nothing is saved."
3. **Compare words** (both): four numbered words in `glance-label` tiles, derived from the handshake by the protocol (the threat model names it). "If they don't, someone may be in the middle. Don't join." **They match** (primary) / **They don't match** (outlined; aborts and deletes everything from the attempt).
4. **Joined:** pick a nickname (random default, regenerate button), see when the group ends, **Go to map**. `Joined` haptic.

## Rules

- No "share code" or "copy" action anywhere: the code is for eyes and cameras in the same place only.
- The check words are mandatory, not a skippable extra; they're cheap and they are the defence against a swapped code.
- Exact handshake, word list and code rotation are threat-model decisions; the screens don't depend on them.
