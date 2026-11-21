# ReportPin

A report on the map: family shape and colour, type icon, time-left ring, and whether it came from one of your groups (verified) or from anyone nearby (unverified). SPEC F4–F7, F9, FR-9.2.

**Status:** Built · **Compose:** `ReportPin(look: PinLook, contentDescription, onClick)` in `core/designsystem/…/component/ReportPin.kt`. `PinLook` holds the family, icon, source (`PinSource.Nearby` or `PinSource.Group`), time left, confirmations, probably gone, yours and could-be-a-trap; the app maps a report type to its family and glyph.

## Anatomy (outside in)

1. **Hit area:** `touch-min` (64dp), always, even when the visual is smaller.
2. **Expiry ring:** see ExpiryIndicator. `expiry-ring` (4dp) stroke, `on-surface` over a `surface` track.
3. **Halo:** `pin-halo` (3dp). Public: `pin-halo` colour. Group: the group's `group-N` colour.
4. **Shape:** by family. `hazard` diamond, `blocked` rounded square, `safe` circle (sizes in the main README).
5. **Icon:** `icon-pin` (24dp), one glyph per type.
6. **Badges:** group monogram top left (group reports only, `label-small` on `group-N`); confirmation count top right when 2 or more, on `inverse-surface`; on a public steering report (Exit open, All clear, Street closed) from another phone, an amber "?" bottom right on `blocked` (extended spec FR-15.2), and the count shows group confirmations only (FR-15.3).

## Source: verified vs unverified

The single most important distinction on the map (SPEC FR-9.2, and the product decision that public reports are in). Three cues, so it survives colour blindness, glare and discreet mode:

| | Group (verified) | Public (unverified) |
|---|---|---|
| Fill | solid family colour | tonal `<family>-container` |
| Edge | none | 3dp **dashed** family colour |
| Halo | group colour | neutral `pin-halo` |
| Badge | group monogram | none |
| TalkBack | "…, from Crew" | "…, from a phone nearby, unverified" |

Solid means "someone in your group signed this"; hollow means "anyone could have sent this".

## States

- **Default**, **new** (one `fastSpatial` pop on arrival), **pressed** (`state-pressed` layer).
- **Ending:** ring under 10% (no colour change: the ring itself says it).
- **Probably gone** (FR-6.4): whole pin at `probably-gone` opacity, 60% greyscale; still tappable. Never removed by votes alone.
- **Yours:** a 2dp `primary` outline 10dp outside the shape, so you can find and withdraw it.
- **Large:** `pin-size-large` (56dp), from Settings > Display.

## Rules

- Fixed colours and shapes per family; users can't change them.
- No text label on the map pin itself (it would collide in a crowd of pins); the type is in the icon, the content description and the detail sheet.
- Content description: "{Type}, {source}, {N} minutes left[, unconfirmed, could be a trap][, {N} confirmations][, probably gone]".
