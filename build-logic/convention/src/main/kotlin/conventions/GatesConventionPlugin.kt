package conventions

import conventions.gates.ImportBoundariesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * Root-project gates that read source files (ADR-0002 and the crypto rule). The dependency and
 * manifest gates live with the module plugins, because they need each module's resolved graph.
 */
class GatesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            check(this == rootProject) { "conventions.gates is applied to the root project only" }
            val packageRoot = providers.gradleProperty("app.packageRoot")

            fun ImportBoundariesTask.common(
                rule: ImportBoundariesTask.Rule,
                include: List<String>,
            ) {
                group = "verification"
                this.rule.set(rule)
                sources.from(
                    fileTree(layout.projectDirectory) {
                        include(include)
                        exclude("**/build/**")
                    },
                )
                rootDirectory.set(layout.projectDirectory)
                this.packageRoot.set(packageRoot)
                // The app's only files allowed to name a transport adapter: where it registers them.
                registrationFiles.set(setOf("TransportRegistration.kt"))
                cryptoPackages.set(CRYPTO_PACKAGES)
            }

            tasks.register<ImportBoundariesTask>("checkTransportBoundaries") {
                description = "Fails if app/ or core/ imports a transport adapter, or an adapter imports another (ADR-0002)."
                common(ImportBoundariesTask.Rule.TRANSPORT, listOf("app/src/**/*.kt", "core/**/src/**/*.kt", "adapters/**/src/**/*.kt"))
            }
            tasks.register<ImportBoundariesTask>("checkCryptoBoundaries") {
                description = "Fails if code outside core/crypto imports a cryptographic API (.ai/rules/crypto.md)."
                common(ImportBoundariesTask.Rule.CRYPTO, listOf("app/src/**/*.kt", "core/**/src/**/*.kt", "adapters/**/src/**/*.kt"))
            }
        }

    companion object {
        /** Cryptographic APIs only core/crypto may import. Add the library the crypto ADR picks. */
        val CRYPTO_PACKAGES =
            listOf(
                "javax.crypto",
                "java.security",
                "android.security.keystore",
                "org.bouncycastle",
                "com.goterl.lazysodium",
                "com.southernstorm.noise",
                "org.signal.libsignal",
                "org.whispersystems",
                "com.google.crypto.tink",
            )
    }
}
