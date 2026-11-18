package io.github.throughthewind.echo.core.mesh.wire

import com.code_intelligence.jazzer.junit.FuzzTest
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

/**
 * The decoder's fuzz target (ADR-0005, spec AC-12, .ai/rules/crypto.md).
 *
 * A plain `./gradlew test` replays the seeds and past findings under
 * `src/test/resources/…/WireFuzzTestInputs/decode/`. With `JAZZER_FUZZ=1` Jazzer mutates them for
 * the time below; CI does that on every pull request. A crash it finds is written next to the seeds:
 * commit it with the fix, and it is replayed on every run from then on.
 */
class WireFuzzTest {
    @FuzzTest(maxDuration = "2m")
    fun decode(data: ByteArray) = WireProperty.check(data)
}

/**
 * Proves the fuzzing step can fail (AGENTS.md: a control that cannot fail loudly is not a control).
 * The same harness runs a decoder with a planted bug, which Jazzer must find. Off unless CI asks for
 * it, because it is meant to fail.
 */
@EnabledIfEnvironmentVariable(named = "ECHO_FUZZ_SELF_TEST", matches = "1")
class WireFuzzSelfTest {
    @FuzzTest(maxDuration = "2m")
    fun planted(data: ByteArray) =
        WireProperty.check(data) { bytes ->
            // A plausible decoder bug: trusting a length byte read off the air.
            if (bytes.size > 3 && bytes[0] == Wire.VERSION.toByte() && bytes[1] == 0x7E.toByte()) {
                ByteArray(bytes[2].toInt()).let { it[bytes[3].toInt() and 0xFF] }
            }
            WireCodec.decode(bytes)
        }
}
