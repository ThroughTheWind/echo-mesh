package conventions.gates

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ForbiddenDependenciesTest {
    private val rules =
        ForbiddenDependencies.parse(
            """
            # comment
            com.google.android.gms
            com.google.firebase   # trailing comment
            com.example:tracker
            """.trimIndent(),
        )

    @Test
    fun `a forbidden group and its subgroups fail`() {
        assertNotNull(rules.match("com.google.android.gms", "play-services-nearby"))
        assertNotNull(rules.match("com.google.firebase.crashlytics", "anything"))
    }

    @Test
    fun `a single forbidden artifact fails and its neighbours pass`() {
        assertNotNull(rules.match("com.example", "tracker"))
        assertNull(rules.match("com.example", "harmless"))
    }

    @Test
    fun `a group that only shares a prefix passes`() {
        assertNull(rules.match("com.google.android.gmsx", "x"))
        assertNull(rules.match("androidx.compose.ui", "ui"))
        assertNull(rules.match("org.jetbrains.kotlin", "kotlin-stdlib"))
    }

    @Test
    fun `malformed and empty rule files are refused`() {
        assertFailsWith<IllegalArgumentException> { ForbiddenDependencies.parse("a:b:c") }
        assertFailsWith<IllegalArgumentException> { ForbiddenDependencies.parse("# nothing\n\n") }
    }

    @Test
    fun `the repository's own rule file parses and forbids Play Services`() {
        val file =
            generateSequence(File("").absoluteFile) { it.parentFile }
                .map { File(it, "gradle/forbidden-dependencies.txt") }
                .first { it.isFile }
        val repoRules = ForbiddenDependencies.parse(file.readText())
        assertNotNull(repoRules.match("com.google.android.gms", "play-services-nearby"))
        assertNotNull(repoRules.match("com.google.firebase", "firebase-analytics"))
        assertNull(repoRules.match("androidx.core", "core-ktx"))
        assertEquals(repoRules.size, file.readLines().count { it.substringBefore('#').isNotBlank() })
    }

    @Test
    fun `the repository's core rule file forbids compression libraries and nothing core needs`() {
        val file =
            generateSequence(File("").absoluteFile) { it.parentFile }
                .map { File(it, "gradle/forbidden-in-core.txt") }
                .first { it.isFile }
        val core = ForbiddenDependencies.parse(file.readText())
        assertNotNull(core.match("org.tukaani", "xz"))
        assertNotNull(core.match("org.apache.commons", "commons-compress"))
        assertNotNull(core.match("com.github.luben", "zstd-jni"))
        assertNull(core.match("org.apache.commons", "commons-lang3"))
        assertNull(core.match("org.jetbrains.kotlinx", "kotlinx-coroutines-core"))
        assertNull(core.match("com.google.crypto.tink", "tink"))
        assertNull(core.match("org.jetbrains.kotlin", "kotlin-stdlib"))
    }
}
