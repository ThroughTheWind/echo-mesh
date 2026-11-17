---
description: >-
  Rules for cryptography and the wire format. Load when editing core/crypto/, or any code that
  encodes or decodes what goes over the air.
paths:
  - "core/crypto/**"
  - "core/mesh/**"
enforcement: gated
---

# Cryptography and the wire format

- **No new primitives and no home-made protocol.** Use the library and the published protocol the
  crypto ADR names, through `:core:crypto` only. `./gradlew checkCryptoBoundaries` fails if any other
  module imports a cryptographic API; add the chosen library's package to its list, never remove one.
- Every change to what goes over the air is **versioned**: a frame starts with its format version,
  and a phone refuses a version it does not know rather than guessing.
- A decoder checks the size before it parses, never allocates from a length it read off the air, and
  never throws past the mesh: a malformed frame from one peer must not knock the phone off the mesh.
  Every decoder has a Jazzer fuzz test, fuzzed in CI on every pull request next to a planted-bug run
  that proves the step can fail (`WireFuzzTest`, ADR-0005). A finding is committed with its fix.
- Nothing in `core/` compresses: `checkCompressionImports` refuses `java.util.zip` and compression
  libraries in source, and `gradle/forbidden-in-core.txt` refuses them on every `:core:*` classpath.
- Signatures and MACs are verified over the exact bytes received (the decoder's `signedBytes`, never a
  re-encoding), before any field is acted on. The decoder only checks layout and ranges.
- Never log a key, a plaintext report, a position or a peer handle. Release builds strip
  `android.util.Log` (`app/proguard-rules.pro`), which is a backstop, not a licence.
- Anything here needs a second reviewer, and the independent security review before 1.0 reads this
  module first.
