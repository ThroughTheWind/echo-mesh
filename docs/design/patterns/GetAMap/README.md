# Get a map

**Purpose:** get an offline map onto the phone before the protest, with or without internet (SPEC F2, US-2, US-3, AC-2, AC-3).

## Paths

1. **Download a city** (FR-2.2a, FR-2.3): a list of regions with sizes. **Open browser** opens the download page in the user's browser; Echo itself has no `INTERNET` permission. The honesty callout (threat model §7): "Downloading tells your network and the map host which region you chose."
2. **Load a map file** (FR-2.2b): the Android file picker, then an import screen with a determinate progress bar ("checking and unpacking"). A file Echo can't read fails with the [import error](../EmptyAndErrorStates/README.md), and nothing changes.
3. **Done:** snackbar "Map loaded: Paris centre"; the map recentres on the region.
4. **Share a map** (FR-2.4): from the [Maps screen](../../screens/MapsScreen/README.md), Share opens Android's share sheet with the file. The sheet says what sharing reveals.

## Where it starts

Onboarding step "Get a map before you go", the [no-map empty state](../EmptyAndErrorStates/README.md) on the map, and Settings > Maps.

## Rules

- Getting map data is always an explicit action (FR-2.2); nothing updates or downloads on its own.
- Regions are listed with their size before anything happens.
