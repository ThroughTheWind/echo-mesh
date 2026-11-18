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

/** The rules every module's runtime classpath is checked against (ADR-0003). */
internal const val FORBIDDEN_EVERYWHERE = "gradle/forbidden-dependencies.txt"

/** The extra rules for `:core:*` modules: no compression library (ADR-0005). */
internal const val FORBIDDEN_IN_CORE = "gradle/forbidden-in-core.txt"

/**
 * Dependency-gate tasks for one resolvable configuration, wired into the lifecycle task: the
 * app-wide rules, and for a `:core:*` module the core rules too.
 */
internal fun Project.checkForbiddenDependencies(configuration: Configuration) {
    register(configuration, "", FORBIDDEN_EVERYWHERE, "ADR-0003, $FORBIDDEN_EVERYWHERE")
    if (path.startsWith(":core:")) {
        register(configuration, "InCore", FORBIDDEN_IN_CORE, "no compression in core, ADR-0005, $FORBIDDEN_IN_CORE")
    }
}

private fun Project.register(
    configuration: Configuration,
    suffix: String,
    rules: String,
    why: String,
) {
    val task =
        tasks.register<ForbiddenDependenciesTask>(
            "checkForbiddenDependencies" + suffix + configuration.name.replaceFirstChar(Char::uppercase),
        ) {
            group = "verification"
            description = "Fails if ${configuration.name} contains an artifact forbidden by $rules."
            configurationName.set(configuration.name)
            rootComponent.set(configuration.incoming.resolutionResult.rootComponent)
            rulesFile.set(rootProject.layout.projectDirectory.file(rules))
            reason.set(why)
        }
    tasks.named(FORBIDDEN_DEPENDENCIES_TASK) { dependsOn(task) }
}
