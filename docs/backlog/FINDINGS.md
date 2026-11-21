**Authoritative for:** what has been noticed and not yet decided, and the disposition of everything closed.
**Not authoritative for:** what to do about any of it — a finding becomes a work item before it becomes work.

# Findings

Things noticed while doing something else. **A finding is the observation; a work item is the
decision.** Recording one must cost almost nothing, or it will not happen — so a finding is a
**row**, not a file. Items are files; findings are rows. The asymmetry is deliberate.

<!-- NEXT-ID: F-013 -->

## Open

| Id | Sev | Pri | What | Evidence | When to act | How to fix |
| --- | --- | --- | --- | --- | --- | --- |
| F-001 | high | next | CI has no slot yet for the Noise test vectors `:core:crypto` must pass (ADR-0004). Decoder fuzzing and the compression ban landed with WI-003. | `.github/workflows/ci.yml` runs the fuzzer and the compression gates, but nothing checks a Noise handshake yet. | When the Noise join is written (WI-017). | Run the cacophony `NKpsk0` vectors as a unit test of `:core:crypto` in the same change as the handshake. |
| F-002 | medium | next | Wi-Fi Direct and Wi-Fi Aware move data over IP sockets, and Android needs `INTERNET` for any socket, which ADR-0003 and `checkManifestPermissions` forbid. | `adapters/wifidirect/README.md`; Android requires `INTERNET` for `socket()`, to be confirmed by transport research. | Before `:adapters:wifidirect` or `:adapters:wifiaware` gets code. | An ADR that either allows `INTERNET` with sockets bound to the peer-to-peer interface only, or keeps Wi-Fi radios out of the MVP. |
| F-003 | high | next | `checkForbiddenDependencies` once passed with a forbidden artifact on the classpath while the configuration cache was on: it saw 2 external modules instead of 7. Not reproduced after clearing `.gradle/configuration-cache`. | Local run of 2026-11-17 (WI-002): `:core:crypto` with `com.google.crypto.tink:tink` planted printed "2 external modules, none forbidden" with the cache reused; `--no-configuration-cache` and a fresh cache both failed correctly on the same build file. | Before trusting a cached local run of the gate; CI starts without a configuration cache, so it is not affected today. | Find the cause, or make the gate read the resolved graph in a way the configuration cache cannot serve stale, and add a self-test that plants a dependency under the cache. |
| F-006 | high | next | A phone already in a group that scans a fresh code for the same group is added again under a new key: the inviter adds a member that will never speak (it counts towards the six) and announces it to the group, while the scanning phone refuses the group it already holds and says "No answer from the other phone". | `GroupChannel.answer` adds the joiner as soon as it sends the welcome; `GroupChannel.join` then fails at `book.adopt`, which refuses a group id it already has. The join request carries a fresh key, so the inviter cannot tell. Found by reading the code while checking a failed second scan on the phones (2026-11-19). | Before the field test (WI-028). | The inviter adds the member only once the newcomer confirms it took the group (a third handshake step, versioned and fuzzed, crypto review); the newcomer says "You're already in this group". A loopback test in which a member scans a fresh code. |
| F-007 | medium | someday | After a panic wipe, Android still lists Echo's permissions (Bluetooth, notifications, location) as granted, so the phone shows Echo was set up, unlike a fresh install. | Phone check of AC-17 on 2026-11-19: data gone 0.2 s after the hold, onboarding back at step 1, permissions still granted in Android's app info. | When the threat model's seized-phone section is next reviewed. | On Android 13+, `revokeSelfPermissionsOnKill` for the runtime permissions as part of the wipe, and say in What Echo protects what remains below 13. |
| F-008 | medium | next | Leaving a group is silent (FR-8.5), so the phone that left stays on every other member's list under its old name, and a phone that leaves and rejoins shows up twice: the group looks bigger than it is, and a ghost reads like a stranger. | Phone check of 2026-11-19: join, leave, join again on the OnePlus left the test group listing three members for two phones. `GroupChannel.leave` destroys the keys and sends nothing. | Before the field test (WI-028): groups will join and leave there. | A signed "left" message from the leaving member before its keys go, so the others drop it from their list; needs a spec change to FR-8.5 (Antoine) and the crypto review, like any new message. |
| F-009 | low | someday | A report reads up to two minutes short of its lifetime as soon as it arrives: an "Exit open" showed "12 min left of 15" about a minute after it was sent. The wire clock is whole minutes (FR-7.3), floored at signing, and the label floors again (`Expiry.wholeMinutes`), so each loses up to 59 s. | Phone check of AC-26 on 2026-11-20; the two phones' clocks agreed within 0.6 s, both on automatic time, so skew is ruled out. `Instant.wireMinute` floors, `instantOfWireMinute` maps back to the minute's start. | When the report detail or pin copy is next revised. | Round the label up ("13 min left" until the last minute), or say "about" in the detail; the wire's minute stays coarse on purpose. A design-copy decision, not a protocol change. |
| F-010 | low | someday | Echo started cold while the phone is locked shows first-run onboarding, as if it had no data: its sealed settings cannot be read until unlock, so they read as defaults. Since WI-077 nothing is lost (changes made then are replayed on the real settings at unlock), but the screen is misleading. | Phone check of 2026-11-20 on build d013c18: Echo launched by adb on the locked Pixel came up on "Step 1 of 4"; its sealed files were untouched, and a launch after unlock showed the group and settings. Opening an app normally needs an unlocked phone, so this needs a launch from the lock screen. | If Echo ever shows anything on the lock screen, or the discreet tile starts the app. | `SettingsStore` exposes that it has not read the file yet, and the app shows a plain "Unlock your phone to open Echo" instead of onboarding until it has. |
| F-011 | high | next | The threat model's 1.0 section (§10: steering reports, low density, diagnostics file, languages, release key custody) was drafted beside the repo on 2026-11-17 and never brought in, yet WI-034 to WI-038 were built. The draft asks for things the build may not do: diagnostics times relative to the session with no date (§10.3), and no language shipped before its fluent safety review is recorded (§10.4), while 0.2.0 ships unreviewed French and Spanish drafts. | `docs/threat-model.md` has no §10 (it says so since WI-040); the draft is in the project folder's `protest-map/docs/threat-model.md` §10. `docs/releases/0.2.0.md` states the translations are unreviewed. Found while bringing in §11 (WI-040), 2026-11-21. | Before 1.0 (WI-033), and before the outside review (WI-030) is scoped. | Bring §10 in through its own item, as WI-040 did for §11, check WI-034 to WI-038 against it, and either fix the build or change the draft with Antoine. |
| F-012 | low | someday | Five fast-tier gate scripts (privacy, claims, design-tokens, fingerprint, locales) cannot run on Windows: they open files at a doubled drive path (`C:\\C:\\...`), so `node .ai/rungs.mjs check` shows 5 failures on a Windows checkout whatever the change. CI on Linux is unaffected. | `node .ai/rungs.mjs check` on Antoine's Windows PC at d3fea33, 2026-11-21: 25 pass, 5 fail, the same with or without a one-line doc edit. The scripts build paths from `new URL('../..', import.meta.url).pathname` (e.g. `scripts/gates/claims.mjs`), which keeps a leading `/C:/` on Windows. | When someone runs the gates on Windows before pushing, or a Windows contributor appears. | Use `fileURLToPath` from `node:url` instead of `.pathname` in every gate script, and run the gates once on a Windows CI runner. |

## Closed

| Id | What | Disposition | Reason |
| --- | --- | --- | --- |
| F-005 | There is no settings screen; items kept deferring to it. | promoted | WI-074 builds the settings screen as designed, taking over the rows other items placed elsewhere; the Maps screen, sharing duration, hazard buzz and licences list stay out of its scope. |
| F-004 | Position sharing paused when Echo left the screen. | promoted | WI-075: Antoine accepted a `location` foreground service while sharing (2026-11-19); `ACCESS_BACKGROUND_LOCATION` stays forbidden, now by the manifest gate. |

---

## Recording one

Use `/record-finding`, or add a row directly. Required:

- **Sev** — `high` (wrong output, data loss, security) · `medium` (wrong behaviour, contained) ·
  `low` (cost, clarity, tidiness)
- **Pri** — `now` · `next` · `someday`. Severity is about the problem; priority is about us.
- **Evidence** — a path, a command, a count. **A finding with no evidence is a hunch**, and the
  next reader cannot tell the difference. If you cannot produce evidence, say so in the row.
- **When to act** — the trigger, not a date. *"Before the next release"*, *"if this recurs"*.
- **How to fix** — enough that someone else could, or an explicit "unknown".

## Closing one

Every finding leaves the Open table by one of three dispositions, and **each carries a written
reason**:

| Disposition | Means | Reason must say |
| --- | --- | --- |
| **promoted** | It became a work item | Which item, and what scope it took |
| **fixed** | It was resolved directly | What changed, and where |
| **dismissed** | It is not a problem, or not one worth solving | *Why not* — this is the one people skip, and it is the one that stops the same observation being recorded again next month |

`node .ai/rungs.mjs check` refuses a closed finding with no reason.

## What this register does not do

- It does not prioritise. A `now` priority is a claim by whoever typed it.
- It does not prove anything is fixed. `fixed` means someone said so.
- It counts what was **recorded**, not what was noticed. A quiet register and an unobserved repo
  look identical from here.
