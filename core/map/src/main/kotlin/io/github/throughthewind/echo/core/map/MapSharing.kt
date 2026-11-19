package io.github.throughthewind.echo.core.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider

/**
 * How map files reach the phone and leave it, with no network code in the app (spec FR-2.2 to
 * FR-2.4, ADR-0006): the user's browser downloads, the system file picker imports, and the share
 * sheet passes a file to another phone.
 */
object MapSharing {
    /** Where to download a region, opened in the user's own browser. Download at home (threat model A6). */
    val downloadPages: List<Uri> =
        listOf(
            Uri.parse("https://download.mapsforge.org/maps/v5/"),
            Uri.parse("https://www.openandromaps.org/en/downloads"),
        )

    /** Opens [page] in the user's browser. The app itself fetches nothing. */
    fun browse(page: Uri): Intent = Intent(Intent.ACTION_VIEW, page).addCategory(Intent.CATEGORY_BROWSABLE)

    /** The system file picker for a map file; needs no permission (ACTION_OPEN_DOCUMENT). */
    fun pick(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")

    /** The share sheet for [entry], so one phone with a map can equip others (FR-2.4). */
    fun share(
        context: Context,
        entry: MapEntry,
    ): Intent {
        val uri = FileProvider.getUriForFile(context, authority(context), entry.file)
        val send =
            Intent(Intent.ACTION_SEND)
                .setType("application/octet-stream")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, null)
    }

    /** The directory the library keeps maps in; the file provider shares this one only. */
    fun directory(context: Context) = context.filesDir.resolve("maps")

    private fun authority(context: Context) = "${context.packageName}.maps"
}
