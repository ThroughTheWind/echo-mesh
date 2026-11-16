# Empty and error states

**Purpose:** make "nothing here" and "something broke" useful: say what's going on, why, and the one thing to do next.

## Empty states

| Where | Message | Action |
|---|---|---|
| Map, no map loaded | "No map here. Reports still show on this grid." (warning callout under the top bar; pins drawn on a 100 m grid) | Get a map |
| Reports near you | "No reports right now", plus how reports appear and vanish | none |
| Groups | "No groups yet", plus what a group is | Create a group / Join by scanning |
| Group roster, nobody seen | "Nobody seen in the last 15 min" | Add someone in person |

Anatomy: centred `icon-lg` glyph, `title-medium` headline, one or two `body-medium` sentences, then actions at the bottom in the thumb zone. No illustrations.

## Errors

| Problem | Where it shows | Message |
|---|---|---|
| Radio off / permission missing | Mesh status chip (error), mesh sheet | "Radio off · Fix" / "Permission needed · Fix" |
| Map import failed | Danger callout on the import screen | What happened, that nothing changed, next step |
| Report rate limit | Disabled picker tile | "Sent 40 s ago" |
| Words don't match on join | Join flow | "Don't join", then the attempt is deleted |

## Rules

- Errors never blame the user and never show a code, a stack trace or a radio's internal name.
- An error that the user can fix has exactly one action, named with the verb.
- Nothing that can be fixed is a toast that disappears.
