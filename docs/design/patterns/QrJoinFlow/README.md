# Join a group by QR

**Purpose:** add someone to a group in person, so that a photo of the code is worth at most one join and every join is visible (SPEC FR-8.2, FR-8.3, FR-12.5, US-4, AC-13, AC-25; threat model §5.5).

## Steps

1. **Show** (any member): a warning callout first, on `blocked-container`: "Show this only face to face. Whoever scans it can read Crew's reports and see shared positions." Then the code, black on white in **every** theme including Discreet (scanners need it), at full window brightness, with `FLAG_SECURE`. The code is one-time and valid for 2 minutes: "One person, one scan · new code in 1:48". It refreshes on its own when it expires or is used.
2. **Scan** (joiner): in-app scanner, no Play Services; camera permission [in context](../PermissionsInContext/README.md).
3. **Joining:** an indeterminate bar and "Keep the two phones close. They talk directly to each other, not through the mesh." This is the Noise handshake over one direct radio hop. Cancel is available.
4. **Joined:** callout "Everyone in Crew sees that you joined", plus when the group ends, and the group's name, prefilled with the inviter's and editable on this phone only. The nickname (random default, FR-8.4) is picked before scanning, because the join carries it to the group (ADR-0009).
5. **Member sees who joined:** the showing phone reports "Yellow Otter joined Crew" and the guidance from the threat model: "Not the person in front of you? Someone else used the code. Make a new group in person and stop using this one." Every other member gets a "Yellow Otter joined" snackbar and a roster entry.
6. **Used code:** a second scan of the same code (a photo, a screenshot that slipped through) fails with "This code was already used" (AC-25).

## Rules

- No "share code" or "copy" anywhere: the code is for eyes and cameras in the same place.
- There's no word-comparison step: the one-time code plus the visible join is the defence (threat model §5.5).
- Failures say what happened and never imply an attack as fact ("may have used it").
