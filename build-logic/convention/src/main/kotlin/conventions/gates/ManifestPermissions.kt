package conventions.gates

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/** The manifest gate's reader (ADR-0003): which permissions a merged manifest asks for. */
object ManifestPermissions {
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    private val PERMISSION_TAGS = listOf("uses-permission", "uses-permission-sdk-23")

    fun declared(manifestXml: String): Set<String> {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                // A manifest never needs a DTD or an external entity; refusing them keeps the
                // parser from reaching outside the file.
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(manifestXml)))
        val root = document.documentElement
        check(root.tagName == "manifest") { "not an Android manifest: the root element is <${root.tagName}>" }
        return PERMISSION_TAGS
            .flatMap { tag ->
                val nodes = root.getElementsByTagName(tag)
                (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(ANDROID_NS, "name") }
            }.filter { it.isNotEmpty() }
            .toSet()
    }

    /** Forbidden permissions that [declared] contains and [allowed] does not excuse. */
    fun violations(
        declared: Set<String>,
        forbidden: Set<String>,
        allowed: Set<String>,
    ): Set<String> = declared intersect (forbidden - allowed)
}
