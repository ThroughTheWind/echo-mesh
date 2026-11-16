# GroupRoster

Your groups, who in them has been seen recently, who's sharing their position, and your own sharing switch (SPEC F8, F10, FR-8.7, FR-10.x).

**Compose:** `GroupList(groups)`, `GroupRoster(group, onShareToggle, onAdd, onLeave)`.

## Groups list

Up to five groups (FR-8.7). Each row: monogram avatar in the group's `group-N` halo, name, "5 seen · you share position" or "ends in 3 h". Below: **Create a group** (tonal), **Join by scanning** (outlined), and the callout "You can't remove someone from a group. Make a new one and share it in person." (FR-8.5).

## Roster

- **Header:** back, group name (`title-large`), "5 members seen · ends tomorrow 18:00", monogram.
- **Share my position** row on `primary-container` with a switch: "Crew sees where you are · stops in 1 h 42" (FR-10.1: off by default, every time; turning it on asks for a duration: 30 min, 1 h, 2 h (default), until mesh off).
- **Members**, one 72dp row each: avatar (initials of their nickname), nickname, then "Sharing · seen 40 s ago · 80 m" or "Not sharing · seen 2 min ago". Over 2 minutes: stale avatar. Not seen for 15 minutes: dropped from the list ("Members not seen for 15 min drop off this list. Nobody's history is kept."). A "show on map" button for members who share.
- **Bottom:** **Add someone in person** (opens QrJoinFlow's show step), **Leave Crew** (outlined, `error` text; confirm sheet: "Leave Crew? Its keys are deleted from this phone. To come back, someone has to show you the code again.").

## Rules

- Nicknames only, default random friendly names ("Yellow Otter", FR-8.4). Never the phone's owner name or contacts.
- No member count beyond "seen recently"; the app can't know who is in a group but silent.
