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

## Scope

In scope: anything that lets someone learn who uses the app, track a phone, read or forge reports
or group traffic, impersonate a phone or a group member, knock phones off the mesh, or recover data
from a seized phone beyond what the threat model says. Also any network call the app makes beyond
peer-to-peer radio, and any dependency that is proprietary or tracks users.

## Status

The app is pre-alpha and has had **no independent security review yet**. One is planned before 1.0,
and 1.0 will not ship without it. Until then, do not rely on the app for your safety.
