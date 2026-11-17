# Accessibility

Accessibility is street usability: the same rules that help a TalkBack user help someone with gloves in the sun.

## Targets

- `touch-min` (64dp) for anything tappable, pins included; `touch-comfort` (72dp) for buttons, rows and tiles; `touch-hero` (88dp) for the Report button and hold to wipe.
- At least `space-md` (12dp) between two targets.
- A visual smaller than its target (a 44dp pin, a 48dp chip) keeps the full hit area.

## Contrast

- Text: 7:1 (WCAG AAA) in Dark and Sunlight, 4.5:1 (AA) in Discreet.
- Non-text marks (pins, halos, outlines, focus rings, map markers): 3:1.
- Gated: `tools/tokens.py check` and `ContrastTest.kt` fail the build on any miss. The gallery's Accessibility card computes the same ratios live.

## Not colour alone

Every meaning carried by colour has a second cue: report families have shapes, public reports have a dashed edge, group reports have a monogram, errors have an icon and words.

## Screen readers

- Pins are buttons whose label says type, source, time left and confirmations ("Tear gas, from Crew, 11 minutes left, 4 confirmations").
- The map offers a "Reports near you" list sorted by distance, so dropping and reading a report never needs a map gesture (SPEC NFR-12).
- Every gesture has a tap alternative. Hold to wipe is deliberate: TalkBack users get a "Wipe everything" custom action using the platform's double-tap-and-hold.

## Focus and input

Hardware keyboards and switch access get a visible `focus` ring (3px `secondary`, 2px offset) on every interactive element. Focus order follows reading order; sheets trap focus until dismissed.

## Text scaling

Sheet, list and settings text scales freely. Pin badges cap at 1.3×. Pins follow Settings > Display > Large pins instead.
