# Field diagnostics

**Purpose:** let a field tester count how the mesh behaved and hand the counts to the team by hand, after reading every line of what they hand over (extended spec F17, FR-17.1 to FR-17.4, NFR-19).

Opened from the [mesh sheet](../../components/MeshStatus/README.md)'s "Field diagnostics" row; the [Settings](../SettingsScreen/README.md) page takes the row over when it exists.

## Structure

[Top app bar](../../components/TopAppBar/README.md) "Field diagnostics" with back and the panic wipe, then one scrolling column:

1. A [switch](../../components/Switch/README.md) row "Count how the mesh behaves", off by default.
2. Three short paragraphs, `body-large`: what it counts; what it never records; where the counts live and when they go.
3. While on: a heading "The whole file", then the export exactly as it will be shared, `body-small` in a monospace face on a `surface-container-high` card, then a primary "Share this file" and a secondary "Refresh".

## Rules

- The text on screen is the file that is shared, byte for byte. Refresh takes a new copy; sharing never takes one the user hasn't seen.
- Share opens the system share sheet. The file goes from memory to the picked app; it's never written to the phone first, so the storage audit ([audits](../../../audits.md)) finds nothing new.
- Turning the switch off forgets every count at once, with no confirm: there's nothing to lose that the user needs.
- No count is shown anywhere else: not on the map, not in the notification.
- Copy: the two privacy sentences cite threat model §7 P4 and P6 in `strings.xml`, under the claims gate.

## Accessibility

- The switch row is one toggle target at the comfortable size, read as "Count how the mesh behaves, switch, off".
- The file is plain text, so TalkBack reads it line by line; the heading lets a reader jump past it to the buttons.
