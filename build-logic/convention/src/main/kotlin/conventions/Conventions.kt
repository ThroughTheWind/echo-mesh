package conventions

import conventions.gates.ForbiddenDependenciesTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.version(name: String): String = libs.findVersion(name).get().requiredVersion

internal fun Project.sdk(name: String): Int = version(name).toInt()

internal val Project.javaVersion: JavaVersion get() = JavaVersion.toVersion(version("jvmTarget"))

internal const val FORBIDDEN_DEPENDENCIES_TASK = "checkForbiddenDependencies"

/**
 * Registers the lifecycle task `checkForbiddenDependencies` once per project and hangs it on `check`.
 * Running it from the root runs it in every module (ADR-0003).
 */
internal fun Project.forbiddenDependenciesLifecycle() =
    tasks
        .register(FORBIDDEN_DEPENDENCIES_TASK) {
            group = "verification"
            description = "Fails if a resolved runtime classpath contains a forbidden artifact (ADR-0003)."
        }.also { lifecycle -> tasks.named("check") { dependsOn(lifecycle) } }

/** One dependency-gate task for one resolvable configuration, wired into the lifecycle task. */
internal fun Project.checkForbiddenDependencies(configuration: Configuration) {
    val task =
        tasks.register<ForbiddenDependenciesTask>(
            "checkForbiddenDependencies" + configuration.name.replaceFirstChar(Char::uppercase),
        ) {
            group = "verification"
            description = "Fails if ${configuration.name} contains a forbidden artifact (ADR-0003)."
            configurationName.set(configuration.name)
            rootComponent.set(configuration.incoming.resolutionResult.rootComponent)
            rulesFile.set(rootProject.layout.projectDirectory.file("gradle/forbidden-dependencies.txt"))
        }
    tasks.named(FORBIDDEN_DEPENDENCIES_TASK) { dependsOn(task) }
}
