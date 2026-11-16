# Motion & haptics

In a crowd, motion is a signal, not decoration: a pin that moves means something changed. Haptics confirm an action through a glove without looking. Discreet mode switches both down.

## Springs

Zapper's spring tokens, unchanged (Compose `spring(dampingRatio, stiffness)`): `fastSpatial` 0.6/800, `defaultSpatial` 0.8/380, `slowSpatial` 0.8/200, `fastEffects` 1.0/3800, `defaultEffects` 1.0/1600, `slowEffects` 1.0/800. Fallback durations: short 100ms, medium 250ms, long 400ms.

## What moves

| Event | Motion | Reduced motion and discreet |
|---|---|---|
| New report arrives | Pin pops from 0.4 to 1 scale, `fastSpatial`, once | Appears without scaling |
| Report expires | Fades out over `medium` | Disappears |
| Probably gone | Cross-fades to `probably-gone` opacity, `defaultEffects` | Instant |
| Expiry ring | Redraws once a minute (never a continuous animation: it would cost battery and draw the eye) | Same |
| Group member moves | Marker glides to the new position, `slowSpatial` | Jumps |
| Sheet open / close | `defaultSpatial` | Instant |
| Hold to wipe | Fill grows linearly over 2s, follows the finger exactly | Same: it is feedback, not decoration |
| Map follows you | Off by default; centre-on-me pans with `slowSpatial` | Jumps |

No looping animation anywhere, including the mesh status (a pulsing dot in a pocket-lit screen is exactly what discreet users don't want, and it costs battery).

## Haptics

Semantic events, mapped to `HapticFeedbackConstants` like Zapper's `Haptics.kt`:

| Event | Constant | When |
|---|---|---|
| Press | VIRTUAL_KEY | Finger down on a button, tile or pin |
| ReportSent | CONFIRM (30+) | The 5s grace period ends and the report goes out |
| ReportCancelled | REJECT (30+) | Cancel during the grace period |
| WipeTick | CLOCK_TICK | Every 0.5s while holding the wipe button |
| WipeDone | LONG_PRESS | Wipe finished (the last thing the app does before the fresh screen) |
| Joined | CONFIRM (30+) | Words matched, you're in the group |
| NearbyHazard | two short `VibrationEffect` pulses | Optional, off by default: a new `hazard` report within 200 m while the app is in front |

Settings > Feedback > Haptics: **Off**, **Light** (default), **Strong**. **Discreet mode forces Off** (SPEC FR-13.1), including the wipe ticks: the fill is the feedback. The app never plays a sound in any mode.
