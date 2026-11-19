**Authoritative for:** what has been noticed and not yet decided, and the disposition of everything closed.
**Not authoritative for:** what to do about any of it — a finding becomes a work item before it becomes work.

# Findings

Things noticed while doing something else. **A finding is the observation; a work item is the
decision.** Recording one must cost almost nothing, or it will not happen — so a finding is a
**row**, not a file. Items are files; findings are rows. The asymmetry is deliberate.

<!-- NEXT-ID: F-006 -->

## Open

| Id | Sev | Pri | What | Evidence | When to act | How to fix |
| --- | --- | --- | --- | --- | --- | --- |
| F-001 | high | next | CI has no slot yet for the Noise test vectors `:core:crypto` must pass (ADR-0004). Decoder fuzzing and the compression ban landed with WI-003. | `.github/workflows/ci.yml` runs the fuzzer and the compression gates, but nothing checks a Noise handshake yet. | When the Noise join is written (WI-017). | Run the cacophony `NKpsk0` vectors as a unit test of `:core:crypto` in the same change as the handshake. |
| F-002 | medium | next | Wi-Fi Direct and Wi-Fi Aware move data over IP sockets, and Android needs `INTERNET` for any socket, which ADR-0003 and `checkManifestPermissions` forbid. | `adapters/wifidirect/README.md`; Android requires `INTERNET` for `socket()`, to be confirmed by transport research. | Before `:adapters:wifidirect` or `:adapters:wifiaware` gets code. | An ADR that either allows `INTERNET` with sockets bound to the peer-to-peer interface only, or keeps Wi-Fi radios out of the MVP. |
| F-003 | high | next | `checkForbiddenDependencies` once passed with a forbidden artifact on the classpath while the configuration cache was on: it saw 2 external modules instead of 7. Not reproduced after clearing `.gradle/configuration-cache`. | Local run of 2026-11-17 (WI-002): `:core:crypto` with `com.google.crypto.tink:tink` planted printed "2 external modules, none forbidden" with the cache reused; `--no-configuration-cache` and a fresh cache both failed correctly on the same build file. | Before trusting a cached local run of the gate; CI starts without a configuration cache, so it is not affected today. | Find the cause, or make the gate read the resolved graph in a way the configuration cache cannot serve stale, and add a self-test that plants a dependency under the cache. |

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
