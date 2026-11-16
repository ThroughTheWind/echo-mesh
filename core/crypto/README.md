# :core:crypto

Identities, group keys, signing and encryption, wrapping one well-reviewed library. Empty until the
threat-model ADR chooses the library and protocol. The only module allowed to import a
cryptographic API: `./gradlew checkCryptoBoundaries` fails on any other. Read
[`.ai/rules/crypto.md`](../../.ai/rules/crypto.md) before adding code here.
