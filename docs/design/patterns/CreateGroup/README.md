# Create a group

**Purpose:** start an affinity group whose keys only exist on phones that met in person (SPEC FR-8.1, FR-8.4, US-4, AC-13).

## Flow

1. **New group:** a [text field](../../components/TextField/README.md) for the name ("Shown on this phone, and to the people you add in person. Never to anyone else.") and lifetime [chips](../../components/Chip/README.md): 6 h, **24 h** (default), 3 days, 7 days (FR-8.1). Callout: "When it ends, its keys are deleted from every phone. Shorter is safer: less to expose if a phone is taken." Primary button names the group: "Create Crew".
2. **Ready:** pick your nickname (random friendly default, regenerate button, FR-8.4). **Add people now** opens the show-code step of [Join a group by QR](../QrJoinFlow/README.md); **Later** returns to the roster.

## Rules

- At most five groups (FR-8.7): with five, "Create a group" is disabled and says "You're in 5 groups, the most Echo allows. Leave one first."
- The group name is local to each phone. A newcomer receives the name of the member who showed the code, inside the encrypted one-hop join only (ADR-0009), and may rename it on their phone; a rename is never sent (Antoine, 2026-11-19).
