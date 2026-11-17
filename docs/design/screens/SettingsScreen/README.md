# Settings screen

The single settings screen (SPEC "Settings screen (summary)"), one scrolling list grouped by section.

## Structure

[Top app bar](../../components/TopAppBar/README.md) "Settings", then sections, each a `label-medium` heading in `primary` followed by a group of [list items](../../components/ListItem/README.md):

1. **Mesh:** on/off, background relaying.
2. **Display:** theme (Auto, Dark, Sunlight), discreet mode, large pins.
3. **Privacy:** default position-sharing duration, include maps in the wipe, "What Echo protects" (reopens onboarding step 2).
4. **Groups:** link to the groups list (also reachable from the map).
5. **Maps:** downloaded and imported maps (size, age, delete), download, import.
6. **Feedback:** haptics (Off, Light, Strong), nearby hazard buzz.
7. **Panic wipe:** one row in `error`, opening the same hold sheet as the map.
8. **About:** version, licence, source link, security contact (shown as text, per SECURITY.md), open-source licences.

## Rules

- Every setting applies immediately; there is no Save.
- A setting that discreet mode overrides shows its effective value and why ("Off while discreet mode is on").
- Choices with more than two options open a sheet of [radio buttons](../../components/RadioAndCheckbox/README.md); the row's support line shows the current value.
