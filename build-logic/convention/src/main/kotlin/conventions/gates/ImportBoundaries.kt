package conventions.gates

/**
 * The source-import gates. Each rule looks only at `import` lines, so it is fast and cannot be fooled
 * by a comment, but a fully qualified name in code slips past it: review still owns that case.
 */
object ImportBoundaries {
    data class Violation(
        val path: String,
        val line: Int,
        val import: String,
    ) {
        override fun toString() = "$path:$line  $import"
    }

    /**
     * ADR-0002: `app/` and `core/` never import a transport adapter, except the app's registration
     * files, and an adapter never imports another adapter.
     *
     * @param path the file's path relative to the repository root, with `/` separators.
     */
    fun transportViolations(
        path: String,
        lines: List<String>,
        packageRoot: String,
        registrationFiles: Set<String>,
    ): List<Violation> {
        val adaptersPackage = "$packageRoot.adapters."
        val ownAdapter =
            path
                .takeIf { it.startsWith("adapters/") }
                ?.split('/')
                ?.getOrNull(1)
        val guarded = path.startsWith("app/") || path.startsWith("core/") || ownAdapter != null
        if (!guarded) return emptyList()
        if (path.startsWith("app/") && path.substringAfterLast('/') in registrationFiles) return emptyList()
        return imports(lines).filter { (_, imported) ->
            imported.startsWith(adaptersPackage) &&
                (ownAdapter == null || !imported.startsWith("$adaptersPackage$ownAdapter."))
        }.map { (line, imported) -> Violation(path, line, imported) }
    }

    /**
     * No home-made cryptography (brief constraint 5, ADR to come from the threat model): only
     * `core/crypto/` may import a cryptographic API, so every primitive in the app is reviewed in
     * one module.
     */
    fun cryptoViolations(
        path: String,
        lines: List<String>,
        cryptoPackages: List<String>,
    ): List<Violation> {
        if (path.startsWith("core/crypto/")) return emptyList()
        return imports(lines)
            .filter { (_, imported) -> cryptoPackages.any { imported == it || imported.startsWith("$it.") } }
            .map { (line, imported) -> Violation(path, line, imported) }
    }

    private val IMPORT = Regex("""^\s*import\s+([\w.`*]+)""")

    private fun imports(lines: List<String>): List<Pair<Int, String>> =
        lines.mapIndexedNotNull { index, line ->
            IMPORT.find(line)?.let { index + 1 to it.groupValues[1].replace("`", "") }
        }
}
