# Permissions in context

**Purpose:** ask for each Android permission once, at the moment its feature needs it, with one sentence of why (SPEC FR-1.2). Never a wall of permissions at first launch: onboarding asks for none.

## The set (minSdk 31)

| Permission | Asked when | Our sentence | If refused |
|---|---|---|---|
| Nearby devices (`BLUETOOTH_SCAN`, `_ADVERTISE`, `_CONNECT`) | First time the user turns the mesh on | "Echo talks to other phones over Bluetooth. Android calls this Nearby devices. Echo doesn't need your location for it." | Mesh chip: "Permission needed · Fix" |
| Notifications (`POST_NOTIFICATIONS`, Android 13+) | Right after, before the service starts | "Android shows a notification while Echo relays in the background. It never shows content on your lock screen." | The service still runs; Android hides its notification |
| Location (`ACCESS_FINE_LOCATION`) | First tap on Centre on me | "Only to draw you on your own map. Your position is never stored, and never sent unless you share it with a group." | No blue dot; everything else works |
| Camera | First time the user opens the scanner | "The camera is used only to read the code, and nothing is saved." | Can't join by QR; says so |
| Background running (battery optimisation exemption) | First time relaying stops unexpectedly, or from the mesh sheet | See [Mesh on and off](../MeshOnOff/README.md) | Relaying may stop when the screen is off |

Because minSdk is 31, Bluetooth never needs location (`neverForLocation`); the copy says so, because people expect the opposite.

## Anatomy

Our own bottom sheet comes first (icon, `headline-small` title, one sentence, **Continue**, **Not now**). Only Continue triggers the Android dialog, so a refusal in our sheet doesn't burn the system prompt.

## Rules

- One permission per sheet, never two at once.
- After a refusal, don't ask again on the next tap; show the fix in context (chip, callout) and let the user choose.
- The location sheet's escape is a real choice, "Keep me off the map", not "Cancel".
