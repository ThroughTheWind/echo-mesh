package conventions.gates

/**
 * The dependency gate's matcher (ADR-0003). Rules come from `gradle/forbidden-dependencies.txt`, one
 * per line: a Maven group (`com.google.firebase`), which also matches its subgroups
 * (`com.google.firebase.crashlytics`), or `group:artifact` for a single artifact. `#` starts a comment.
 */
class ForbiddenDependencies(
    private val rules: List<Rule>,
) {
    data class Rule(
        val group: String,
        val artifact: String?,
        val line: Int,
    ) {
        override fun toString() = if (artifact == null) group else "$group:$artifact"
    }

    /** The rule that forbids [group]:[artifact], or null when it is allowed. */
    fun match(
        group: String,
        artifact: String,
    ): Rule? =
        rules.firstOrNull { rule ->
            val groupMatches = group == rule.group || group.startsWith(rule.group + ".")
            groupMatches && (rule.artifact == null || rule.artifact == artifact)
        }

    val size: Int get() = rules.size

    companion object {
        fun parse(text: String): ForbiddenDependencies {
            val rules =
                text.lines().mapIndexedNotNull { index, raw ->
                    val line = raw.substringBefore('#').trim()
                    if (line.isEmpty()) return@mapIndexedNotNull null
                    val parts = line.split(':')
                    require(parts.size <= 2 && parts.all { it.isNotBlank() && it.none(Char::isWhitespace) }) {
                        "forbidden-dependencies line ${index + 1} is not 'group' or 'group:artifact': $raw"
                    }
                    Rule(parts[0], parts.getOrNull(1), index + 1)
                }
            require(rules.isNotEmpty()) { "forbidden-dependencies has no rules; an empty list forbids nothing" }
            return ForbiddenDependencies(rules)
        }
    }
}
