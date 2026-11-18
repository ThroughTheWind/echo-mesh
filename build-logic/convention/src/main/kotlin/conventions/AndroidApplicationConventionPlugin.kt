package conventions

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import conventions.gates.ManifestPermissionsTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

/**
 * Permissions the app may never ask for unless an ADR allows it (ADR-0003). `allowed` is set in
 * app/build.gradle.kts, next to the ADR that justifies each entry, and is empty by default.
 */
abstract class ManifestPermissionsExtension {
    abstract val forbidden: SetProperty<String>
    abstract val allowed: SetProperty<String>
}

/** The `:app` module: SDK levels, the dependency gate and the merged-manifest permission gate. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("com.android.application")

            val permissions =
                extensions.create<ManifestPermissionsExtension>("manifestPermissions").apply {
                    forbidden.convention(setOf("android.permission.INTERNET"))
                    allowed.convention(emptySet())
                }

            extensions.configure<ApplicationExtension> {
                compileSdk = sdk("compileSdk")
                defaultConfig {
                    minSdk = sdk("minSdk")
                    targetSdk = sdk("targetSdk")
                }
                compileOptions {
                    sourceCompatibility = javaVersion
                    targetCompatibility = javaVersion
                }
                // F-Droid and reproducible builds: no Google-signed dependency metadata blob (ADR-0003).
                dependenciesInfo {
                    includeInApk = false
                    includeInBundle = false
                }
            }

            optInToKotlinTime()
            forbiddenDependenciesLifecycle()
            val manifestLifecycle =
                tasks.register("checkManifestPermissions") {
                    group = "verification"
                    description = "Fails if a merged manifest asks for a forbidden permission (ADR-0003)."
                }
            tasks.named("check") { dependsOn(manifestLifecycle) }

            extensions.configure<ApplicationAndroidComponentsExtension> {
                onVariants { variant ->
                    checkForbiddenDependencies(variant.runtimeConfiguration)

                    val capitalized = variant.name.replaceFirstChar(Char::uppercase)
                    val manifestCheck =
                        tasks.register<ManifestPermissionsTask>("check${capitalized}ManifestPermissions") {
                            group = "verification"
                            description = "Fails if the ${variant.name} merged manifest asks for a forbidden permission."
                            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                            forbidden.set(permissions.forbidden)
                            allowed.set(permissions.allowed)
                        }
                    manifestLifecycle.configure { dependsOn(manifestCheck) }
                    // An APK with a forbidden permission is never assembled, not only never merged.
                    tasks.matching { it.name == "assemble$capitalized" }.configureEach { dependsOn(manifestCheck) }
                }
            }
        }
}
