---
id: WI-076
title: Keep two phones in range linked over Bluetooth
type: bug
status: in_progress
branch: claude/project-thread-67cn4e
created: 2026-11-20
updated: 2026-11-20
related: [WI-011, WI-028, FR-3.4, AC-4, AC-5]
epic: WI-029
children: []
---

## Proposal (rationale)

On the phone check of 2026-11-19 two phones side by side, Bluetooth on and Echo open, lost each other at about 00:32 and found each other again on their own about 30 minutes later; they lost each other again at 01:07. Restarting the mesh did not help. The BLE transport had three ways to stay apart for a whole session or longer: only the lower token connects, so if that phone cannot hear the other nobody connects until a rotation happens to swap the order; a scan the stack stopped was swallowed and stayed stopped for the session; and a server that died, or an advertisement the stack refused, left the phone advertising nothing anyone could reach until the next rotation. Needed before the field test (WI-028).

## Decision

`accepted` — 2026-11-20, Antoine's standing instruction to continue through each phase; a mesh that drops its only neighbour for half an hour fails AC-4 in the field.

## Plan

### Requirements

- Two phones that can hear each other in either direction are linked within `fallbackAfter` plus a connect, without waiting for a rotation.
- A scan the stack stops is started again after `retryAfter`, and counted in field diagnostics.
- A server that dies or an advertisement the stack refuses starts a new session after `retryAfter`.
- Still one link per pair in the usual case: the lower token connects first; the higher waits before connecting itself.

### Impacts

- `adapters/ble`: `BleTransport`, `AndroidBleRadio`; `core/mesh`: `LinkFailure.SCAN`.
- Nothing on the air changes: same advertisement, same tokens and rotation.

### Approach

- The higher token, on hearing a lower one, waits `fallbackAfter` (15 s) and connects if no link to that token came. A duplicate is refused by the existing one-link-per-token rule.
- The scan runs in a loop that restarts it; the accept loop completes the session's `broken` signal, which ends the session early; `AndroidBleRadio.advertise` waits for the stack's answer and throws on a refusal.

### Acceptance criteria / tests

- `BleTransportTest`: a phone that hears nothing still gets a link from the other, in both token orders, without a rotation; a stopped scan is restarted; a dead server starts a new session within seconds. Each fails with its fix removed.
- On the phones (with the other phone checks): two phones side by side stay linked for an hour with screens going on and off.

### Out of scope

- Why the phone's scan stopped in the first place (OEM throttling after screen off is the leading guess): the field diagnostics now count it, and the field test measures it.

## Execution

2026-11-20.

- As planned. The fallback re-arms when the same phone is heard again after it fired, so a link that breaks mid-session is remade by either phone.
- The tests were checked against the transport with each fix removed: each fails then.

## Review
