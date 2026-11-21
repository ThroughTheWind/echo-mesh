#!/usr/bin/env python3
"""Check a Bluetooth capture for anything stable that Echo puts on the air (AC-19, WI-027).

Usage:
    python3 -I scripts/audit/air_scan.py btsnoop_hci.log
    python3 -I scripts/audit/air_scan.py --self-test

The capture is an Android HCI snoop log (btsnoop format) taken on a phone that is **not running Echo**
and kept scanning next to the one that is (docs/audits.md, AC-19). The log holds every advertising
report the scanning phone's controller received, with the advertiser's address as it was on the air:
Android does not translate it at this layer, even for a paired device.

It keeps only Echo's reports (its service UUID in the advertisement, or its service data in the scan
response), then checks, over the whole capture:

- no advertiser address is seen over more than 10 minutes;
- no service data value (token and PSM) is seen over more than 10 minutes;
- token and PSM change together, and no token or PSM comes back after it changed;
- one address never carries two tokens, so a rotation cannot be linked through the address;
- no Echo advertisement or scan response carries a device name.

A capture is only evidence if the scan kept hearing the phone: a controller that drops duplicate
reports, or a scan Android throttled, would make every address look short-lived. So a gap of more
than 2 minutes between Echo reports, or a capture under 30 minutes, makes the result inconclusive.

Exit status 1 when a check fails, 2 when the capture holds no Echo report or is inconclusive (a
blind capture is not a pass), 0 otherwise. It prints counts and durations, never a token, a PSM or an address in full.
"""

import argparse
import struct
import sys
import uuid

SERVICE_UUID = uuid.UUID("6f1c0e2a-7b3d-4e59-9a61-2d8b5c4e7a10")
SERVICE_LE = SERVICE_UUID.bytes[::-1]  # 128-bit UUIDs are little-endian on the air
LIMIT_SECONDS = 10 * 60
MAX_GAP_SECONDS = 2 * 60
MIN_CAPTURE_SECONDS = 30 * 60
TOKEN_BYTES = 4
PSM_BYTES = 2

AD_UUID128_PARTIAL = 0x06
AD_UUID128_COMPLETE = 0x07
AD_SHORT_NAME = 0x08
AD_COMPLETE_NAME = 0x09
AD_SERVICE_DATA_128 = 0x21

BTSNOOP_EPOCH_OFFSET_US = 0x00DCDDB30F2F8000  # microseconds from year 0 to 1970


class Report:
    def __init__(self, seconds, address, data):
        self.seconds = seconds
        self.address = address
        self.data = data


def ad_structures(data):
    """The (type, value) pairs of advertising data, stopping at the first malformed one."""
    out = []
    i = 0
    while i < len(data):
        length = data[i]
        if length == 0 or i + 1 + length > len(data):
            break
        out.append((data[i + 1], data[i + 2 : i + 1 + length]))
        i += 1 + length
    return out


def reports_in_event(seconds, event):
    """Advertising reports in one HCI event (code, length, parameters), legacy and extended."""
    if len(event) < 3 or event[0] != 0x3E:
        return []
    params = event[2:]
    sub = params[0]
    out = []
    if sub == 0x02 and len(params) >= 2:  # LE Advertising Report
        count = params[1]
        if count != 1:
            # The multi-report form lays fields out as arrays; Android controllers send one at a time.
            return [None]
        body = params[2:]
        if len(body) < 9:
            return []
        address = bytes(body[2:8])
        length = body[8]
        out.append(Report(seconds, address, bytes(body[9 : 9 + length])))
    elif sub == 0x0D and len(params) >= 2:  # LE Extended Advertising Report
        count = params[1]
        i = 2
        for _ in range(count):
            if i + 24 > len(params):
                break
            address = bytes(params[i + 3 : i + 9])
            length = params[i + 23]
            out.append(Report(seconds, address, bytes(params[i + 24 : i + 24 + length])))
            i += 24 + length
    return out


def read_btsnoop(blob):
    """Every advertising report in a btsnoop file, and the number of multi-report events skipped."""
    if blob[:8] != b"btsnoop\0":
        raise ValueError("not a btsnoop file")
    datalink = struct.unpack(">I", blob[12:16])[0]
    if datalink not in (1001, 1002):
        raise ValueError(f"unknown btsnoop datalink {datalink}")
    reports, skipped = [], 0
    i = 16
    while i + 24 <= len(blob):
        _, included, flags, _, stamp = struct.unpack(">IIIIq", blob[i : i + 24])
        packet = blob[i + 24 : i + 24 + included]
        i += 24 + included
        if datalink == 1002:  # H4: the first byte is the packet type, 0x04 an event
            if not packet or packet[0] != 0x04:
                continue
            event = packet[1:]
        else:  # unencapsulated: flags bit 1 marks a command or event, bit 0 the direction
            if flags & 0x03 != 0x03:
                continue
            event = packet
        seconds = (stamp - BTSNOOP_EPOCH_OFFSET_US) / 1_000_000
        for r in reports_in_event(seconds, event):
            if r is None:
                skipped += 1
            else:
                reports.append(r)
    return reports, skipped


def echo_fields(report):
    """(is Echo, service data payload or None, has a device name) for one report."""
    is_echo, payload, named = False, None, False
    for kind, value in ad_structures(report.data):
        if kind in (AD_UUID128_PARTIAL, AD_UUID128_COMPLETE):
            if any(value[k : k + 16] == SERVICE_LE for k in range(0, len(value) - 15, 16)):
                is_echo = True
        elif kind == AD_SERVICE_DATA_128 and value[:16] == SERVICE_LE:
            is_echo = True
            payload = bytes(value[16:])
        elif kind in (AD_SHORT_NAME, AD_COMPLETE_NAME):
            named = True
    return is_echo, payload, named


def span(times):
    return max(times) - min(times)


def check(reports):
    """Problems found, and a summary, over the Echo reports among [reports]."""
    echo = []
    for r in reports:
        is_echo, payload, named = echo_fields(r)
        if is_echo:
            echo.append((r, payload, named))
    summary = {"echo_reports": len(echo)}
    if not echo:
        return None, summary
    problems = []
    first = min(r.seconds for r, _, _ in echo)
    last = max(r.seconds for r, _, _ in echo)
    summary["capture_minutes"] = round((last - first) / 60, 1)

    by_address, by_payload, tokens_of_address = {}, {}, {}
    for r, payload, named in echo:
        by_address.setdefault(r.address, []).append(r.seconds)
        if named:
            problems.append(f"a device name in an Echo report at minute {(r.seconds - first) / 60:.1f}")
        if payload is not None:
            if len(payload) != TOKEN_BYTES + PSM_BYTES:
                problems.append(f"service data of {len(payload)} bytes, expected {TOKEN_BYTES + PSM_BYTES}")
                continue
            by_payload.setdefault(payload, []).append(r.seconds)
            tokens_of_address.setdefault(r.address, set()).add(payload[:TOKEN_BYTES])

    for address, times in by_address.items():
        if span(times) > LIMIT_SECONDS:
            problems.append(f"an address (..{address[0]:02x}) seen over {span(times) / 60:.1f} min")
    for payload, times in by_payload.items():
        if span(times) > LIMIT_SECONDS:
            problems.append(f"one token seen over {span(times) / 60:.1f} min")
    for address, tokens in tokens_of_address.items():
        if len(tokens) > 1:
            problems.append(f"one address (..{address[0]:02x}) carried {len(tokens)} tokens, linking rotations")

    # Sessions in the order they appeared: a token or PSM must not come back, and they change together.
    sessions = sorted(by_payload.items(), key=lambda kv: min(kv[1]))
    tokens, psms = [p[:TOKEN_BYTES] for p, _ in sessions], [p[TOKEN_BYTES:] for p, _ in sessions]
    if len(set(tokens)) != len(tokens):
        problems.append("a token came back after it changed")
    if len(set(psms)) != len(psms):
        problems.append("a PSM came back after it changed, or did not change with its token")

    summary["addresses"] = len(by_address)
    summary["tokens"] = len(by_payload)
    summary["rotations"] = max(len(by_payload) - 1, 0)
    summary["longest_address_minutes"] = round(max(span(t) for t in by_address.values()) / 60, 1)
    if by_payload:
        summary["longest_token_minutes"] = round(max(span(t) for t in by_payload.values()) / 60, 1)
    summary["device_name_seen"] = any(n for _, _, n in echo)
    times = sorted(r.seconds for r, _, _ in echo)
    gap = max((b - a for a, b in zip(times, times[1:])), default=0)
    summary["longest_gap_minutes"] = round(gap / 60, 1)
    summary["inconclusive"] = []
    if gap > MAX_GAP_SECONDS:
        summary["inconclusive"].append(f"{gap / 60:.1f} min with no Echo report: the scan was not continuous")
    if last - first < MIN_CAPTURE_SECONDS:
        summary["inconclusive"].append(f"the capture covers {(last - first) / 60:.1f} min, under 30")
    return problems, summary


# --- self-test: synthetic captures, so a checker that matches nothing cannot pass ---------------


def _adv(address, data):
    params = bytes([0x02, 1, 0x00, 0x01]) + address + bytes([len(data)]) + data + bytes([0xC0])
    return bytes([0x04, 0x3E, len(params)]) + params


def _btsnoop(events):
    out = b"btsnoop\0" + struct.pack(">II", 1, 1002)
    for seconds, packet in events:
        stamp = BTSNOOP_EPOCH_OFFSET_US + int(seconds * 1_000_000)
        out += struct.pack(">IIIIq", len(packet), len(packet), 0x03, 0, stamp) + packet
    return out


def _echo_pair(address, token, psm, name=False):
    adv = bytes([17, AD_UUID128_COMPLETE]) + SERVICE_LE
    rsp = bytes([1 + 16 + 6, AD_SERVICE_DATA_128]) + SERVICE_LE + token + psm
    if name:
        rsp += bytes([5, AD_COMPLETE_NAME]) + b"Pixe"
    return [_adv(address, adv), _adv(address, rsp)]


def _capture(sessions, step=30):
    """sessions: (address, token, psm, minutes, name) in order; one advert pair every [step] s."""
    events, t = [], 0.0
    for address, token, psm, minutes, name in sessions:
        end = t + minutes * 60
        while t < end:
            for p in _echo_pair(address, token, psm, name):
                events.append((t, p))
            t += step
    # Noise from another device, which must be ignored.
    events.append((1.0, _adv(b"\x01" * 6, bytes([3, 0x03, 0x0F, 0x18]))))
    return _btsnoop(events)


def self_test():
    a, b, c = b"\xa1" * 6, b"\xb2" * 6, b"\xc3" * 6
    t1, t2, t3 = b"\x01\x02\x03\x04", b"\x05\x06\x07\x08", b"\x09\x0a\x0b\x0c"
    p1, p2, p3 = b"\x00\x81", b"\x00\x83", b"\x00\x85"

    def run(sessions):
        reports, _ = read_btsnoop(_capture(sessions))
        return check(reports)

    good, summary = run([(a, t1, p1, 9, False), (b, t2, p2, 9, False), (c, t3, p3, 9, False), (b"\xd4" * 6, b"\x0d" * 4, b"\x00\x87", 4, False)])
    assert good == [] and summary["inconclusive"] == [], (good, summary)
    assert summary["rotations"] == 3 and summary["tokens"] == 4, summary
    sparse = check(read_btsnoop(_capture([(a, t1, p1, 31, False)], step=600))[0])[1]
    assert any("not continuous" in i for i in sparse["inconclusive"]), sparse
    short = run([(a, t1, p1, 9, False)])[1]
    assert any("under 30" in i for i in short["inconclusive"]), short
    cases = [
        ([(a, t1, p1, 12, False)], "seen over"),
        ([(a, t1, p1, 5, False), (a, t2, p2, 5, False)], "linking rotations"),
        ([(a, t1, p1, 5, False), (b, t2, p1, 5, False)], "PSM came back"),
        ([(a, t1, p1, 5, False), (b, t2, p2, 5, False), (c, t1, p3, 5, False)], "token came back"),
        ([(a, t1, p1, 5, True)], "device name"),
    ]
    for sessions, expected in cases:
        problems, _ = run(sessions)
        assert any(expected in p for p in problems), (expected, problems)
    blind, _ = check(read_btsnoop(_btsnoop([(0.0, _adv(b"\x01" * 6, b"\x02\x01\x06"))]))[0])
    assert blind is None
    print("air scan self-test: pass")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("capture", nargs="?")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    if not args.capture:
        parser.error("give the btsnoop capture")
    with open(args.capture, "rb") as f:
        reports, skipped = read_btsnoop(f.read())
    problems, summary = check(reports)
    print(f"{len(reports)} advertising reports read, {skipped} multi-report events skipped")
    if problems is None:
        print("no Echo report in the capture: the scan is blind", file=sys.stderr)
        return 2
    inconclusive = summary.pop("inconclusive")
    for key, value in summary.items():
        print(f"{key}: {value}")
    for p in problems:
        print(f"FAIL: {p}")
    for i in inconclusive:
        print(f"INCONCLUSIVE: {i}")
    print(f"{len(problems)} problems")
    if problems:
        return 1
    return 2 if inconclusive else 0


if __name__ == "__main__":
    sys.exit(main())
