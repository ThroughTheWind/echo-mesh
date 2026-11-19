package io.github.throughthewind.echo.mesh

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Hands the field diagnostics file to the app the user picks in the share sheet (FR-17.3), straight
 * from memory through a pipe: the file is never written to this phone's storage (FR-17.4), so the
 * storage audit (docs/audits.md, AC-18) still finds only sealed files and maps.
 *
 * It serves exactly the text the user reviewed, frozen by [share], and nothing once the app is
 * restarted. Not exported: only an app the share sheet grants it to can read it, once.
 */
class DiagnosticsProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = MIME

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val bytes = reviewed ?: return null
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(columns, 1).apply {
            addRow(
                columns.map {
                    when (it) {
                        OpenableColumns.DISPLAY_NAME -> FILE_NAME
                        OpenableColumns.SIZE -> bytes.size.toLong()
                        else -> null
                    }
                },
            )
        }
    }

    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("read only")
        val bytes = reviewed ?: throw FileNotFoundException("nothing to share")
        val (read, write) = ParcelFileDescriptor.createPipe()
        Thread {
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(write).use { it.write(bytes) }
            } catch (_: IOException) {
                // The reader went away: nothing was kept, nothing to clean up.
            }
        }.start()
        return read
    }

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        const val FILE_NAME = "echo-diagnostics.txt"
        private const val MIME = "text/plain"

        @Volatile
        private var reviewed: ByteArray? = null

        /** The share sheet for [text], exactly as the user just reviewed it. */
        fun share(
            context: Context,
            text: String,
        ): Intent {
            reviewed = text.encodeToByteArray()
            val uri = Uri.parse("content://${context.packageName}.diagnostics/$FILE_NAME")
            val send =
                Intent(Intent.ACTION_SEND)
                    .setType(MIME)
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            return Intent.createChooser(send, null)
        }

        /** Forgets the reviewed text: diagnostics were turned off or wiped (FR-17.4). */
        fun forget() {
            reviewed = null
        }
    }
}
