**Authoritative for:** how to run a field test of Echo and hand in its diagnostics files (extended spec FR-17.5, WI-028).
**Not authoritative for:** what the diagnostics may hold ([extended spec F17](SPEC-EXTENDED.md), held by a test, NFR-19) or the values tuned from the results (the item that tunes them).

# Field testing

A field test measures what no simulation can: how far reports travel in a real crowd, how long they
take, and what the mesh costs in battery. Echo has no telemetry and never will (ADR-0003), so the
data comes from files that testers export by hand, after reading them.

## Before the day

1. Every phone runs the same release candidate. Write down the version and each phone's model and
   Android version (not its owner).
2. Charge every phone to 100% and close other apps that use Bluetooth.
3. Add the same offline map to every phone at home (threat model N6: the download reveals the
   region).
4. Agree the density levels to try, each for at least 20 minutes:
   - **sparse:** phones 30 to 50 m apart, a few in range of each other;
   - **street:** a march's density, phones 5 to 15 m apart;
   - **packed:** a rally's density, phones within a few metres.
5. Use test groups with throwaway names and nicknames. Nothing about the test needs a real one.

## On the day

1. On each phone: open the mesh sheet, tap **Field diagnostics**, turn **Count how the mesh
   behaves** on. Counting starts at minute 0 from that tap.
2. Turn the mesh on. Carry the phone the way a marcher would: in a pocket or a bag, screen off,
   except when reporting.
3. Keep a paper log, in minutes since you turned diagnostics on, of: the density level and its
   start and end, each report you send (type only), and anything unusual (a phone restarted, a
   tunnel, a battery saver). The diagnostics file never holds the time of day, so the log is how
   the team lines files up.
4. Measure:
   - **propagation (NFR-1):** a report sent by one phone, and the minute it shows on the others;
     the file's delivery delays give the spread;
   - **battery (NFR-9):** the file reads the battery every 10 minutes; note when a phone was
     charged or unplugged;
   - **low density (FR-16.4):** at the sparse level, note in the log when the mesh chip says "Few
     phones" and whether reports were in fact missing; the file's `peers` column shows how many
     phones each phone heard, to set the threshold (now under 3 in 5 minutes) and its copy;
   - **report lifetimes in use:** whether reports were still useful when they expired, from the
     testers' own judgment, in the log.
5. If Android stops the mesh, the file counts it (`service_killed`). If the whole app was stopped
   and reopened, its counts start again and the new file says `killed_before_session,yes`: export
   before turning diagnostics on again if you can.

## After, before leaving

1. On each phone: **Field diagnostics**, read **The whole file**. It holds only counts by minute:
   phones nearby, reports received and passed on, delivery delays, failed connections by kind, the
   battery, and whether Android stopped the mesh. If anything else is in it, do not share it:
   report it as a security issue ([SECURITY.md](../SECURITY.md)).
2. Tap **Share this file** and send it to the test's coordinator, by hand, over a channel you
   trust. Name it after the phone's number on your sheet, not its owner.
3. Turn diagnostics off; that forgets every count. Use the panic wipe if the phone carried test
   groups you no longer need.

## What the team does with the files

The work item for the test (WI-028) puts a results table for propagation and battery against
NFR-1 and NFR-9 in its Execution, and changes each tuned constant in one pull request citing it.
Files are kept with the test's notes only as long as that item is open.
