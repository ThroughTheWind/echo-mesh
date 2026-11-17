# What Echo protects

**Purpose:** the complete, honest list of what Echo protects and what it doesn't, reachable at any time from Settings > Privacy and from onboarding's "Read everything" (SPEC FR-1.1, FR-1.3, NFR-14).

## Content

Two lists, word for word from threat model §7 (meaning fixed there, wording here):

**It protects**
1. Group reports, positions and nicknames can be read only by people who joined the group in person.
2. A group report shows which member sent it, and nobody can fake another member's report.
3. Nothing you send carries your name, number or a fixed ID, and Echo's radio signals change regularly.
4. Echo keeps no history of where you or others were. Reports disappear when they expire.
5. A panic wipe makes everything Echo stored unreadable within 2 seconds.
6. There's no server, no account, and no internet use except a map download you start yourself.

**It doesn't protect**
1. Anyone nearby with a radio receiver can tell your phone is running Echo.
2. Public reports can be fake, and their confirmations can be faked in bulk.
3. Anyone in your group can read everything sent to it, see shared positions, and pass them on. If a member's phone is taken, assume the group is exposed and make a new one in person.
4. If your phone is taken while unlocked, whoever has it can read your groups and post as you until you wipe it or your group makes a new one.
5. Someone with many receivers may guess roughly where a report was first sent.
6. Downloading a map reveals the region to your network and the map host.

## Rules

- A change to either list starts in the threat model; this page follows it. The outside security review checks this page against §7 (threat model §8, item 8).
- Green check and amber warning icons, never a red cross: "doesn't protect" is information, not failure.
