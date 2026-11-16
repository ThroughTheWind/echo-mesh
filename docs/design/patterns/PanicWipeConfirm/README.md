# PanicWipeConfirm

Erase everything in one deliberate move: tap the wipe button, then hold for 2 seconds (SPEC F11, FR-11.1–11.4).

**Compose:** `PanicWipeSheet(onWiped)`, `HoldToConfirmButton(duration = 2.seconds)`.

## Flow

1. **Tap** the wipe button (`delete_forever`, `error` colour) on the map's top bar, or the Quick Settings tile. It sits at the top on purpose: hard to hit from a pocket, one reach away when you mean it. That's the first action.
2. **Hold** the `touch-hero` (88dp) button at the bottom of the sheet: "Hold to wipe". It fills left to right with `error` over 2 s, the label switching to "Keep holding · 1.2 s"; `WipeTick` haptic every 0.5 s (none in discreet mode). Letting go at any point empties it and nothing happens. That's the second action (FR-11.1: at most two).
3. **At 2 s** the wipe runs (key destroyed first, FR-11.2), and the app opens on its first-launch screen with no "wiped" message (FR-11.3): a fresh install is the point.

## Sheet copy

- Title: "Wipe everything?" (`headline-small`).
- "Groups and their keys, all reports, settings. It looks like a fresh install afterwards." When Settings > Privacy > "Include maps" is on, add "and maps".
- "Copies on other phones stay. Nothing is sent to say you wiped." (FR-11.4)
- **Cancel** text button under the hold button.

## Rules

- No second "Are you sure?": the hold *is* the confirmation, and a dialog would cost seconds.
- The hold target is in the thumb zone; the sheet's scrim tap cancels.
- TalkBack: custom action "Wipe everything", which uses the platform's double-tap-and-hold; the 2 s countdown is announced once.
- The fill uses a second clipped copy of the label so the text stays readable over both colours.
