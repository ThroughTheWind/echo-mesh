# Mesh on and off

**Purpose:** let someone turn the mesh on with one tap, trust that it keeps relaying in a pocket, and turn it off completely (SPEC F3, US-5, US-6, AC-5).

## Flow

1. **Off on first launch** (FR-3.1). The [mesh status](../../components/MeshStatus/README.md) chip reads "Mesh off · Turn on"; one tap turns it on.
2. **First time only:** the [Nearby devices and Notifications sheets](../PermissionsInContext/README.md) appear before the radio starts.
3. **On:** the foreground service starts (only with the app visible, never at boot, FR-3.3) and the [system notification](../../components/SystemNotification/README.md) appears. Snackbar "Mesh on · relaying in the background". The chip shows "Looking for phones", then the count.
4. **Off:** from the mesh sheet's switch. Turning off stops all radio use, stops position sharing (FR-10.1, with a snackbar saying so if it was on) and removes the notification. Reports already on the map stay until they expire; nothing new arrives.
5. **Relaying stopped unexpectedly** (FR-3.3a): if the service dies while the mesh is meant to be on, the chip turns to "Relaying stopped · Fix". The sheet explains in one sentence, offers **Allow background running** (the Android battery-optimisation exemption) and **Steps for your phone**, bundled per-manufacturer instructions picked by `Build.MANUFACTURER`, shown offline.

## Rules

- The chip is the only on/off control on the map; it's never hidden.
- No auto-start: after a reboot or a wipe the mesh is off until the user turns it on.
- Reports are in memory only (FR-12.2): if Android kills the app, the map is empty on return and the [empty state](../EmptyAndErrorStates/README.md) says why.
