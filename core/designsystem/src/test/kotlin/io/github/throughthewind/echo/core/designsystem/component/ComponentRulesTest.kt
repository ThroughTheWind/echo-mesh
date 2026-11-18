package io.github.throughthewind.echo.core.designsystem.component

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** The rules in components/ExpiryIndicator and components/MeshStatus that are numbers, not pixels. */
class ComponentRulesTest {
    @Test
    fun `time left is in whole minutes, and under a minute is said so rather than counted down`() {
        assertEquals(14, Expiry.wholeMinutes(14.minutes + 59.seconds))
        assertEquals(1, Expiry.wholeMinutes(1.minutes))
        assertEquals(null, Expiry.wholeMinutes(59.seconds))
    }

    @Test
    fun `the ring is the share of the current lifetime left, clamped`() {
        assertEquals(0.5f, Expiry.fraction(10.minutes, 20.minutes))
        assertEquals(1f, Expiry.fraction(30.minutes, 20.minutes))
        assertEquals(0f, Expiry.fraction((-1).minutes, 20.minutes))
        assertEquals(0f, Expiry.fraction(1.minutes, 0.minutes))
    }

    @Test
    fun `nearby counts stop at 20+ because precision past that is false`() {
        assertEquals("12", nearbyCount(12))
        assertEquals("20", nearbyCount(20))
        assertEquals("20+", nearbyCount(21))
    }
}
