# Drop a report

**Purpose:** drop a report in two taps, where the thing is, to the right audience (SPEC F4, FR-4.2, FR-4.3, FR-4.6, US-7, US-8, US-13, AC-4).

## Flow

1. **Report** ([FAB](../../components/FloatingActionButton/README.md)) opens the sheet and shows a crosshair at the map's centre, shifted up so it stays above the sheet. The report goes **at the crosshair, never at your GPS position** (FR-4.2, US-8). Pan the map to move it.
2. **Pick a type:** a 3 × 3 grid of tiles grouped by family. Below it, the audience [segmented button](../../components/SegmentedButton/README.md): **Everyone nearby** / **{Group} only**, defaulting to the last choice (FR-4.3). With several groups, the group segment opens a list of radio buttons; "Everyone nearby" carries "unverified".
3. **Confirm:** the chosen type in `glance-title` with its lifetime, and one hero button naming the audience: "Send to Crew" (lock) or "Send to everyone nearby". A secondary outlined button switches audience.
4. **Grace period:** the snackbar "Tear gas sending to Crew in 5 s · Cancel" (`send-grace`). A report that has left the phone can only be withdrawn, so the cancel window comes first and the copy says "sending". Then the `ReportSent` haptic.

## What a report holds (FR-4.4)

A type and a place, and nothing else you type: there is no text field anywhere in the flow, so a report can't carry a name, a phone number or a message. Time, lifetime and hops are added by the app.

## Rate limit (FR-4.6)

At most one report per type per minute. A limited tile is disabled and reads "Sent 40 s ago" until the minute is up.

## Accessibility

Tiles are buttons named by type. The crosshair is announced as "Report will be placed at the map centre". The whole flow works from [Reports near you](../../screens/ReportsNearYou/README.md) too, without map gestures.
