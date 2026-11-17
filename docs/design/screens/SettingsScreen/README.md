# Settings screen

The single settings screen (SPEC "Settings screen (summary)", FR-1.3, FR-2.6), one scrolling list grouped by section.

## Structure

[Top app bar](../../components/TopAppBar/README.md) "Settings", then sections, each a `label-medium` heading in `primary` followed by a group of [list items](../../components/ListItem/README.md):

1. **Mesh:** on/off; "Keep running in the background" (the battery exemption, [Mesh on and off](../../patterns/MeshOnOff/README.md)).
2. **Groups and maps:** links to the groups list ([Group roster](../GroupRoster/README.md)) and the [Maps screen](../MapsScreen/README.md).
3. **Display:** theme (Auto, Dark, Sunlight), discreet mode, large pins.
4. **Privacy:** default position-sharing duration, [What Echo protects](../WhatEchoProtects/README.md).
5. **Feedback:** haptics (Off, Light, Strong), nearby hazard buzz.
6. **Panic wipe:** one row in `error`, opening the same hold sheet as the map.
7. **About:** version, licence, source link, security contact (as text, per SECURITY.md), open-source licences.

## Rules

- Every setting applies immediately; there's no Save.
- A setting that discreet mode overrides shows its effective value and why ("Off while discreet mode is on").
- Choices with more than two options open a sheet of [radio buttons](../../components/RadioAndCheckbox/README.md); the row's support line shows the current value.
