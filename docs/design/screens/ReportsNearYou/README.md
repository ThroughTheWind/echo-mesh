# Reports near you

**Purpose:** every report the phone holds, as a list sorted by distance. It's the path for TalkBack users and for anyone who'd rather read than scan a map (SPEC NFR-12, FR-9.3, US-9, US-14).

## Structure

- [Top app bar](../../components/TopAppBar/README.md) "Reports near you", opened from the map's "Reports near you" action (a TalkBack action on the map, and the list icon next to the filter).
- The same filter as the map, as a [segmented button](../../components/SegmentedButton/README.md): **Everything** / **My groups** (FR-9.3).
- Rows: the pin (with its verified or unverified style and ring), type, then "distance direction · source · confirmations"; compact time left at the end. A group row names the member (threat model §7). Tapping opens [Report detail](../../patterns/ReportDetail/README.md).
- Bottom: **Report at the map centre** opens [Drop a report](../../patterns/ReportPicker/README.md) without needing the map.

## Rules

- Sorted by distance from you, or from the map centre when location is off.
- Probably-gone rows sit at the end, faded, with "probably gone" in the support line.
- Updates in place; it never reorders while TalkBack focus is on a row.
