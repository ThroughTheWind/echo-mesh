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

Reached from the gear on the map's top bar. Screens it opens (groups, What Echo protects, field diagnostics) return to Settings on Back.

### As built (WI-074)

- **Mesh** also holds the Field diagnostics row ([Field diagnostics](../FieldDiagnostics/README.md)). Turning the mesh on returns to the map, which shows the permission sheets in context first ([Permissions in context](../../patterns/PermissionsInContext/README.md)); turning it off happens here.
- **Maps** opens the get-a-map sheet until the [Maps screen](../MapsScreen/README.md) exists.
- **Language** sits between Feedback and Panic wipe: on Android 13 and later it opens Android's language setting for Echo (extended spec FR-18.1); before 13 it says Echo follows the phone's language.
- Not yet shown, because the features don't exist: default position-sharing duration, nearby hazard buzz, open-source licences. "Also delete maps" stays in the wipe sheet.
- The source link is text, not a link: nothing in Echo opens the network.

## Rules

- Every setting applies immediately; there's no Save.
- A setting that discreet mode overrides shows its effective value and why ("Off while discreet mode is on").
- Choices with more than two options open a sheet of [radio buttons](../../components/RadioAndCheckbox/README.md); the row's support line shows the current value.
