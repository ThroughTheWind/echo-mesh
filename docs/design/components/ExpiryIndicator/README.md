# ExpiryIndicator

Time left at a glance (SPEC F7, FR-7.2). Reports vanish on their own, so people need to see how fresh something is without opening it.

**Compose:** `ExpiryRing(fraction)` (drawn by ReportPin), `ExpiryBar(left, total, refreshed)`, `timeLeftLabel(left)`.

## Variants

- **Ring** (on every pin): a 4dp arc around the pin, starting at 12 o'clock and shrinking clockwise as time runs out. `on-surface` on a `surface` track, so it reads on any map colour. Redrawn once a minute, not animated.
- **Bar** (detail sheet): `title-medium` "14 min left", "of 20" on the right, an 8dp bar beneath. Under 10% left the bar turns `blocked`. When confirmations refreshed it (FR-6.3), a line under it says so and shows the hard maximum ("max 40 min").
- **Compact** (lists, TalkBack): "14m", `label-small` on `surface`.

## Rules

- Fraction is time left over the *current* lifetime (after refreshes), so a confirmed report's ring refills.
- Show minutes, never seconds (seconds invite staring); under one minute say "under 1 min".
- At expiry the pin fades out (`medium`) and is deleted, not hidden (FR-7.1, FR-12.4).
