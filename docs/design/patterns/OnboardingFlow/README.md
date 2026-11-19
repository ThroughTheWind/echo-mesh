# Onboarding

**Purpose:** first launch says plainly what Echo protects and what it doesn't, gets a map, and points at the two safety controls (SPEC F1, FR-1.1, FR-1.3, US-1, AC-1). It asks for **no permissions**: those come [in context](../PermissionsInContext/README.md) (FR-1.2).

## Steps

1. **Welcome:** map glyph, "See what's happening around you, without the internet", one sentence on how. **Get started**.
2. **What it protects / what it doesn't:** four lines each, every one a claim from threat model §7 (NFR-14):
   - Protects: group reports, positions and nicknames readable only by people who joined in person; nothing you send carries your name, number or a fixed ID; no history, reports disappear at expiry; no server, account or internet except a map download you start.
   - Doesn't: anyone nearby with a radio receiver can tell your phone runs Echo; public reports and their confirmations can be fake; your group can read what's sent to it and pass it on; a phone taken unlocked can be read until wiped.
   - **I understand** (no skip) and **Read everything**, which opens [What Echo protects](../../screens/WhatEchoProtects/README.md) with the full list.
3. **Get a map before you go:** the two paths of [Get a map](../GetAMap/README.md), or **Skip for now**.
4. **Before you go:** the mesh starts off; reports expire; walking with the mesh on carries reports across gaps to the people you meet (carry-and-deliver, extended spec FR-16.3); discreet mode; panic wipe. **Open the map**.

## Rules

- Four screens (FR-1.1 asks for 3 to 5). Steps 1 and 2 can't be skipped; 3 can.
- After a panic wipe the app starts here again, identical to a fresh install (FR-11.3).
- Copy changes go through threat model §7 first.
