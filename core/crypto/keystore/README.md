# :core:crypto:keystore

The Android Keystore half of `:core:crypto` (Android library). It holds the key that seals what the
app stores (spec FR-12.1, threat model §5.6): AES-256-GCM, hardware-backed (StrongBox where present),
usable only while the phone is unlocked, used through Tink. It lives under `core/crypto/` because it
imports a cryptographic API, and is its own module because `:core:crypto` runs on the plain JVM.
