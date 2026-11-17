# Empty and error states

**Purpose:** make "nothing here" and "something's wrong" useful: say what's going on, why, and the one thing to do next.

## Empty states

| Where | Message | Action |
|---|---|---|
| Map, no map loaded | "No map here. Reports still show on this grid." (warning callout; pins drawn on a 100 m grid) | Get a map |
| Map, after a restart (FR-12.2) | "Reports live only in memory, so the map starts empty after Echo restarts. Phones nearby send them again." Shown once per restart, until the first report arrives | none |
| Reports near you | "No reports right now", plus how reports appear and vanish | none |
| Groups | "No groups yet", plus what a group is | Create a group / Join by scanning |
| Group roster, nobody seen | "Nobody seen in the last 15 min" | Add someone in person |
| Maps | "No maps yet" | Download a city / Load a map file |

Anatomy: centred `icon-lg`, `title-medium` headline, one or two `body-medium` sentences, actions at the bottom in the thumb zone. No illustrations.

## Errors

| Problem | Where it shows | Message |
|---|---|---|
| Bluetooth off / permission missing | [Mesh status](../../components/MeshStatus/README.md) chip | "Bluetooth off · Fix" / "Permission needed · Fix" |
| Relaying stopped by Android | Mesh status chip, then [Mesh on and off](../MeshOnOff/README.md) | "Relaying stopped · Fix" |
| Phone clock disagrees with received reports (FR-7.3) | Danger callout under the top bar | "Your phone's clock looks wrong, so reports may vanish early or not show. Fix the time" (opens Android date settings) |
| Map import failed | Danger callout on the import screen | What happened, that nothing changed, next step |
| Report rate limit | Disabled picker tile | "Sent 40 s ago" |
| Used join code | [Join a group by QR](../QrJoinFlow/README.md) | "This code was already used" |
| Five groups already | Groups screen | "You're in 5 groups, the most Echo allows. Leave one first." |

## Rules

- Errors never blame the user and never show a code, a stack trace or a radio's internal name.
- An error the user can fix has exactly one action, named with its verb.
- Nothing fixable is a toast that disappears.
