# OnboardingFlow

First launch: say plainly what the app protects and what it doesn't, ask for the one permission it needs with its reason, and get an offline map (SPEC F1, FR-1.1, F2, US-1–US-3).

**Compose:** `OnboardingScreen(step)`, one per step, with a page indicator and the primary button at the bottom.

## Steps

1. **Welcome:** map glyph (`icon-lg`), "See what's happening around you, without the internet" (`headline-medium`), one sentence on how (phones pass reports to each other, no server, no account). **Get started**.
2. **What it protects / what it doesn't:** two short lists, green checks then amber warnings. Draft lines, final wording from the threat model:
   - Protects: works with no internet or server; group reports readable only by your group; reports expire and the phone keeps no history of where you were; the ID your phone sends keeps changing.
   - Doesn't: someone nearby with the right equipment can tell a phone is running the app; public reports can be wrong or fake; your group sees its reports and shared positions; a phone taken unlocked before a wipe can be read.
   - **I understand**. No skip: this is the screen that stops people taking risks they otherwise wouldn't.
3. **Find phones nearby:** why the Nearby devices permission is needed, and why some Android versions also ask for location ("the app never stores or sends your position unless you share it with a group"). **Allow** / **Not now** (the mesh chip then shows "Permission needed · Fix").
4. **Get a map before you go:** **Download a city** (opens the browser, per FR-2.3, then imports; size shown) or **Load a map file** (from a friend). **Skip for now**.
5. **Two things to know:** discreet mode (where the toggle is) and panic wipe (tap, then hold 2 s). **Open the map**.

## Rules

- No account, no email, no analytics consent: there is nothing to consent to.
- Five screens maximum (FR-1.1 says 3 to 5). Steps 1 and 2 are mandatory; the rest can be skipped and are reachable later from Settings.
- After a panic wipe the app starts here again, identical to a fresh install.
