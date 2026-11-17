# Position sharing

**Purpose:** let group members find each other, with sharing that is opt-in every time, visible while on, and stops by itself (SPEC F10, US-15, AC-15).

## Flow

1. **Start:** from the group roster's "Share my position" row (or the map's group filter). A sheet names the group and offers a duration as [radio buttons](../../components/RadioAndCheckbox/README.md): 30 min, 1 hour, **2 hours** (default, FR-10.1), until the mesh is off. Callout: "Crew sees where you are, updated about every minute. Each update expires after a few minutes, and nothing is stored on any phone." Button "Start sharing". Location permission is asked [in context](../PermissionsInContext/README.md) if needed.
2. **While sharing** (FR-10.4): the `primary-container` bar under the top bar ("Sharing your position with **Crew** · 1 h 42 left · Stop") on every map view, and the [notification](../../components/SystemNotification/README.md) when the app is in the background. Stop is one tap, with no confirm.
3. **Stops on its own** (FR-10.1): when time is up, the mesh is turned off, or the app is killed. A snackbar says why the next time the map is visible.
4. **Their side** (FR-10.3, AC-15): the member's avatar with its age. Over 2 minutes: the stale style. When the last update expires, the marker disappears. Nothing older is ever shown.

## Rules

- Sharing is per group; sharing with two groups shows two bars stacked, each with its own Stop.
- Your own dot is drawn only on your own map; sharing never changes what strangers see.
