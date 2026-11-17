package conventions.gates

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Fails if one resolved configuration contains a forbidden artifact (ADR-0003). */
abstract class ForbiddenDependenciesTask : DefaultTask() {
    @get:Input
    abstract val configurationName: Property<String>

    @get:Input
    abstract val rootComponent: Property<ResolvedComponentResult>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val rulesFile: RegularFileProperty

    @TaskAction
    fun check() {
        val rules = ForbiddenDependencies.parse(rulesFile.get().asFile.readText())
        val seen = mutableSetOf<ResolvedComponentResult>()
        val offenders = mutableListOf<String>()
        var modules = 0

        // Walk the whole graph, so a tracker pulled in transitively is found and the path to it shown.
        fun visit(
            component: ResolvedComponentResult,
            path: List<String>,
        ) {
            if (!seen.add(component)) return
            val id = component.id
            val here = path + id.displayName
            if (id is ModuleComponentIdentifier) {
                modules++
                rules.match(id.group, id.module)?.let { rule ->
                    offenders += "${id.displayName} (rule '$rule')\n      via ${here.joinToString(" -> ")}"
                }
            }
            component.dependencies
                .filterIsInstance<ResolvedDependencyResult>()
                .forEach { visit(it.selected, here) }
        }
        visit(rootComponent.get(), emptyList())

        val name = "${path.substringBeforeLast(':')}:${configurationName.get()}"
        if (modules == 0) {
            // Every module resolves at least the Kotlin standard library. Seeing nothing means the
            // gate looked at the wrong configuration, not that the module is clean.
            throw GradleException("$name resolved no external module at all: the dependency gate is blind")
        }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "$name contains forbidden dependencies (ADR-0003, gradle/forbidden-dependencies.txt):\n" +
                    offenders.joinToString("\n") { "  $it" },
            )
        }
        logger.lifecycle("$name: $modules external modules, none forbidden by ${rules.size} rules")
    }
}

/** Fails if a variant's merged manifest asks for a forbidden permission (ADR-0003). */
@CacheableTask
abstract class ManifestPermissionsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val forbidden: SetProperty<String>

    @get:Input
    abstract val allowed: SetProperty<String>

    @TaskAction
    fun check() {
        val declared = ManifestPermissions.declared(mergedManifest.get().asFile.readText())
        val violations = ManifestPermissions.violations(declared, forbidden.get(), allowed.get())
        if (violations.isNotEmpty()) {
            throw GradleException(
                "The merged manifest ${mergedManifest.get().asFile} asks for ${violations.sorted()}. " +
                    "The app talks only to other phones over peer-to-peer radio (ADR-0003); a permission " +
                    "is allowed only by an ADR, recorded in app/build.gradle.kts.",
            )
        }
        logger.lifecycle("$path: ${declared.size} permissions, none forbidden")
    }
}

/** Fails if a Kotlin file imports across a boundary that an ADR closes. */
@CacheableTask
abstract class ImportBoundariesTask : DefaultTask() {
    enum class Rule { TRANSPORT, CRYPTO }

    @get:Input
    abstract val rule: Property<Rule>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Input
    abstract val packageRoot: Property<String>

    @get:Input
    abstract val registrationFiles: SetProperty<String>

    @get:Input
    abstract val cryptoPackages: ListProperty<String>

    @TaskAction
    fun check() {
        val root = rootDirectory.get().asFile
        val files = sources.files.sorted()
        if (files.isEmpty()) throw GradleException("$path found no Kotlin source to check: the gate is blind")
        val violations =
            files.flatMap { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                val lines = file.readLines()
                when (rule.get()) {
                    Rule.TRANSPORT -> {
                        ImportBoundaries.transportViolations(relative, lines, packageRoot.get(), registrationFiles.get())
                    }

                    Rule.CRYPTO -> {
                        ImportBoundaries.cryptoViolations(relative, lines, cryptoPackages.get())
                    }
                }
            }
        if (violations.isNotEmpty()) {
            val why =
                when (rule.get()) {
                    Rule.TRANSPORT -> {
                        "app/ and core/ reach transports only through the MeshTransport contract, and adapters " +
                            "never import each other (ADR-0002). Only these app files may name an adapter: " +
                            registrationFiles.get().sorted().joinToString()
                    }

                    Rule.CRYPTO -> {
                        "Only core/crypto/ may import a cryptographic API, so every primitive is reviewed in one " +
                            "place (.ai/rules/crypto.md)"
                    }
                }
            throw GradleException("$why.\n" + violations.joinToString("\n") { "  $it" })
        }
        logger.lifecycle("$path: ${files.size} files, no import crosses the boundary")
    }
}
