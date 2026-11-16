## What changed and why

## How it was tested

Which phones (model and Android version), and how many at once? Simulated on `:adapters:loopback` only?

## Checklist

- [ ] `./gradlew spotlessCheck test lint` and `node .ai/rungs.mjs check` pass
- [ ] Docs and the changelog are updated if users would notice
- [ ] No new dependency is proprietary, tracks users, calls the network or is GPL-3.0-incompatible, and no `INTERNET` permission (ADR-0003)
- [ ] Radio-specific code stays in its `adapters/<radio>` module, cryptography in `core/crypto` (ADR-0002, crypto rule)
- [ ] Nothing new is logged, stored or sent that could identify a person, a phone or a place beyond what the threat model allows
- [ ] Not a security fix: those are reported and fixed privately first ([SECURITY.md](../SECURITY.md))
