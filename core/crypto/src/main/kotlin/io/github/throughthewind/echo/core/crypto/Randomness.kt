package io.github.throughthewind.echo.core.crypto

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/** Unpredictable randomness for the rest of the app, which may not import java.security itself. */
object Randomness {
    /**
     * A [Random] backed by the platform's [SecureRandom]: for anything another phone must not
     * predict, such as a summary's salt (ADR-0008) and the relay's timing jitter.
     */
    fun secure(): Random = SecureRandom().asKotlinRandom()
}
