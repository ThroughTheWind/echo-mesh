#!/usr/bin/env python3
"""Search a pulled copy of Echo's storage for anything that must never be there (AC-18, WI-027).

Usage:
    python3 -I scripts/audit/storage_scan.py <pulled-directory> --lat 48.8675 --lon 2.3632 \
        --text "AuditCrew" --text "Yellow Otter"

It reads every file under the directory and looks for each needle in the encodings a leak would
use: UTF-8 and UTF-16 text, the wire's 1/10 000-degree integers (big- and little-endian), and the
decimal forms a log or JSON would hold. Exit status 1 when anything is found, 2 when the directory
holds no file (a blind scan is not a pass), 0 otherwise. It prints where each hit is, never the
surrounding bytes.
"""

import argparse
import os
import struct
import sys

DEGREE_UNITS = 10_000


def needles(lats, lons, texts):
    found = []
    for t in texts:
        found.append((f"text {t!r} (UTF-8)", t.encode("utf-8")))
        found.append((f"text {t!r} (UTF-16LE)", t.encode("utf-16-le")))
    for label, values in (("latitude", lats), ("longitude", lons)):
        for v in values:
            units = round(v * DEGREE_UNITS)
            found.append((f"{label} {v} (wire units, big-endian)", struct.pack(">i", units)))
            found.append((f"{label} {v} (wire units, little-endian)", struct.pack("<i", units)))
            found.append((f"{label} {v} (double, big-endian)", struct.pack(">d", v)))
            found.append((f"{label} {v} (double, little-endian)", struct.pack("<d", v)))
            for digits in (3, 4):
                found.append((f"{label} {v} (decimal, {digits} places)", f"{v:.{digits}f}".encode()))
    return found


def scan(directory, wanted):
    files = []
    for base, _, names in os.walk(directory):
        files.extend(os.path.join(base, n) for n in names)
    hits = []
    for path in files:
        with open(path, "rb") as f:
            data = f.read()
        for label, needle in wanted:
            at = data.find(needle)
            if at >= 0:
                hits.append(f"{os.path.relpath(path, directory)} at byte {at}: {label}")
    return files, hits


def self_test():
    import tempfile

    with tempfile.TemporaryDirectory() as d:
        with open(os.path.join(d, "clean"), "wb") as f:
            f.write(os.urandom(64))
        wanted = needles([48.8675], [2.3632], ["AuditCrew"])
        files, hits = scan(d, wanted)
        assert len(files) == 1 and not hits, hits
        with open(os.path.join(d, "leak"), "wb") as f:
            f.write(b"\0" + struct.pack(">i", 488675) + "AuditCrew".encode("utf-16-le"))
        _, hits = scan(d, wanted)
        assert any("wire units, big-endian" in h for h in hits), hits
        assert any("UTF-16LE" in h for h in hits), hits
    print("storage scan self-test: pass")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("directory", nargs="?")
    parser.add_argument("--lat", type=float, action="append", default=[])
    parser.add_argument("--lon", type=float, action="append", default=[])
    parser.add_argument("--text", action="append", default=[])
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    if not args.directory or not (args.lat or args.lon or args.text):
        parser.error("give the pulled directory and at least one --lat, --lon or --text")
    files, hits = scan(args.directory, needles(args.lat, args.lon, args.text))
    if not files:
        print("no file under the directory: the scan is blind", file=sys.stderr)
        return 2
    for h in hits:
        print(h)
    print(f"{len(files)} files scanned, {len(hits)} hits")
    return 1 if hits else 0


if __name__ == "__main__":
    sys.exit(main())
