package conventions

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * An Android library module (ADR-0001): data, map, designsystem and the radio transports. The
 * namespace is derived from the module path, so it cannot drift from the package convention.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                namespace = namespaceFor(this@with)
                compileSdk = sdk("compileSdk")
                defaultConfig.minSdk = sdk("minSdk")
                compileOptions {
                    sourceCompatibility = javaVersion
                    targetCompatibility = javaVersion
                }
                testOptions.unitTests.all { it.useJUnitPlatform() }
            }

            dependencies {
                // Android unit tests annotate with org.junit.jupiter.api.Test: kotlin.test.Test is a
                // typealias that does not resolve on an Android variant's test classpath.
                add("testImplementation", platform(libs.findLibrary("junit-bom").get()))
                add("testImplementation", libs.findLibrary("junit-jupiter").get())
                add("testImplementation", libs.findLibrary("kotlin-test").get())
                add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
            }

            forbiddenDependenciesLifecycle()
            extensions.configure<LibraryAndroidComponentsExtension> {
                onVariants { variant -> checkForbiddenDependencies(variant.runtimeConfiguration) }
            }
        }
}

/** `:core:designsystem` → `<packageRoot>.core.designsystem` (AGENTS.md, Conventions). */
internal fun namespaceFor(project: Project): String {
    val root = project.providers.gradleProperty("app.packageRoot").get()
    return root + project.path.replace(':', '.')
}
