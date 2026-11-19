# Security policy

People may rely on this app when they are at risk, so a vulnerability can put someone in danger.
**Please report it privately, never in a public issue, pull request or discussion.**

## How to report

Use GitHub's private vulnerability reporting: the **Security** tab of this repository, then
**Report a vulnerability**. Only the maintainers can read the report. If that is not possible for
you, open a public issue that says only "please contact me about a security issue", with no details.

Helpful to include: what an attacker can do, what they need (radio range, a seized phone, a place in
a group…), the app version and phone model, and steps or code to reproduce it.

## What happens next

- We acknowledge the report within 7 days.
- We agree with you on a fix and a disclosure date. We credit you in the release notes unless you
  prefer not to be named.
- Fixes ship as a new release on GitHub Releases. We do not quietly patch security issues.

## Checking that your copy is ours

The release signing certificate's SHA-256 fingerprint, the same in the README and every release note:

<!-- signing-fingerprint -->
No signed release yet. The first one publishes its fingerprint here.
<!-- /signing-fingerprint -->

To check an APK before installing it: `apksigner verify --print-certs echo.apk` (Android SDK build
tools) prints `Signer #1 certificate SHA-256 digest`, which must match.

## One person holds the key

One maintainer holds the release signing key (extended spec FR-19.1). That is a known risk
(FR-19.5): whoever coerces or compromises that person could sign a malicious update that phones
accept as ours. Watch for a release no changelog or public discussion announced, and report it
here. Before 2.0 we revisit adding a second key holder or co-signing.

The release workflow signs with a copy of the key kept in this repository's encrypted GitHub
Actions secrets, so control of the maintainer's GitHub account is also enough to sign. The master
copy and its backup are offline.

## Scope

In scope: anything that lets someone learn who uses the app, track a phone, read or forge reports
or group traffic, impersonate a phone or a group member, knock phones off the mesh, or recover data
from a seized phone beyond what the threat model says. Also any network call the app makes beyond
peer-to-peer radio, and any dependency that is proprietary or tracks users.

## Status

The app is pre-alpha and has had **no independent security review yet**. One is planned before 1.0,
and 1.0 will not ship without it. Until then, do not rely on the app for your safety.
