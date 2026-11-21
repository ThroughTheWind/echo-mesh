**Authoritative for:** how to check, before each release, that Echo leaves nothing readable in storage and nothing stable on the air (AC-18, AC-19).
**Not authoritative for:** what Echo may claim ([threat model §7](threat-model.md)) or how to cut the release ([releasing](releasing.md)).

# Storage and radio audits

Run both before every release candidate, on two reference phones, the OPPO included. Record the
evidence (the file listing, the scan summary, the capture summary) in the release's work item. A
leak found here becomes its own work item; the audit does not fix it.

## AC-18: nothing readable in storage

1. Install the **debug** build of the candidate (only a debug build allows `run-as`). Start from a
   fresh install.
2. Use it the way a marcher would for about 20 minutes with a second phone: create a group named
   `AuditCrew` with the nickname `Yellow Otter`, join it from the second phone, send public and group
   reports, confirm and dismiss, share your position, mute and unmute a member. Write down the
   positions you reported and stood at (four decimals is enough).
3. Leave the app in the background, then pull its storage. The debug build's id ends in `.debug`
   (`applicationIdSuffix` in `app/build.gradle.kts`); `run-as` on the release id fails with
   "package not debuggable". With both phones on USB, pick one with `adb -s <serial>`.

   ```sh
   adb shell run-as io.github.throughthewind.echo.debug tar -cf - . > echo-storage.tar
   mkdir echo-storage && tar -xf echo-storage.tar -C echo-storage
   ```

4. List it. Expect only `files/sealed/` (the sealed settings and one `group-<hex>` per group, no
   name in clear) and `files/maps/` (maps you added). Anything else, a database, `shared_prefs`
   with content, a cache file, a log, is a finding.

   ```sh
   find echo-storage -type f | sort
   ```

5. Scan every file for what must never be there. The scanner looks for each text in UTF-8 and
   UTF-16, and each coordinate in the wire's units, as doubles and as decimals:

   ```sh
   python3 -I scripts/audit/storage_scan.py echo-storage \
       --lat 48.8675 --lon 2.3632 --text AuditCrew --text "Yellow Otter"
   ```

   Exit 0 with "0 hits" passes. Give every position and name you used; a scan with no needle refuses
   to run, and a directory with no file is reported as blind, not as a pass.
6. Run `python3 -I scripts/audit/storage_scan.py --self-test` once, so the scanner is known to find
   a planted leak.

What this cannot see: keys in the sealed files are ciphertext under the Keystore key, and the
Keystore key never leaves the Keystore. The tests in `GroupRecordTest` and
`PositionSharingTest` check, on every build, that a group record holds no position and that only
group records are kept.

## AC-19: nothing stable on the air

1. Capture BLE advertising near one phone running Echo with the mesh on, for 30 minutes, with an
   open-source sniffer (for example an nRF52840 dongle with the nRF Sniffer for Bluetooth LE and
   Wireshark). Keep the phone still and nothing else running Echo in range.
2. Filter on Echo's service UUID (`docs/transports.md`, section 3) and export, per advertisement:
   time, advertiser address, and the service data (the token and PSM).
3. Check, over the whole capture:
   - no advertiser address lasts more than 10 minutes (Android rotates it; Echo never reads or
     works around it);
   - no service data value lasts more than 10 minutes, and token and PSM change together;
   - the device name never appears in any advertisement or scan response;
   - after each rotation, no field of the new advertisement equals one from the old.
4. Expected and not a finding: the service UUID itself is constant, which is how phones find each
   other. That is threat model §7 N1: anyone nearby with a radio receiver can tell the phone runs
   Echo.

Record the capture's length, the number of rotations seen, the longest-lived address and token, and
"no device name" in the work item.
