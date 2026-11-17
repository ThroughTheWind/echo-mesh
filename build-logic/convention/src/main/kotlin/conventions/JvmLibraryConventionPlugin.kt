package conventions

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * A plain Kotlin/JVM module (ADR-0001): model, mesh, crypto and the loopback transport, so protocol
 * code is tested on the JVM against simulated phones, without an emulator.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
            }
            extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions.jvmTarget.set(JvmTarget.fromTarget(version("jvmTarget")))
                // kotlin.time.Instant is the model's clock type; harmless once it is stable.
                compilerOptions.optIn.add("kotlin.time.ExperimentalTime")
            }

            dependencies {
                add("testImplementation", platform(libs.findLibrary("junit-bom").get()))
                add("testImplementation", libs.findLibrary("junit-jupiter").get())
                add("testImplementation", libs.findLibrary("kotlin-test").get())
                add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
            }
            tasks.withType<Test>().configureEach { useJUnitPlatform() }

            forbiddenDependenciesLifecycle()
            checkForbiddenDependencies(configurations.getByName("runtimeClasspath"))
        }
}
