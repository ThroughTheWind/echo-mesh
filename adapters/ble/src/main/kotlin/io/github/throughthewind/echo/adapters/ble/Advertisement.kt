package io.github.throughthewind.echo.adapters.ble

import java.util.UUID

/**
 * What an app phone advertises (spec FR-3.4, threat model §5.1): the app's fixed service UUID, and as
 * service data a random token and the PSM of its L2CAP server. Both change together at every
 * rotation, with the advertising set restarted so the OS gives it a new address too. Six bytes, so
 * the service data fits a legacy scan response on every phone.
 */
internal data class Advertisement(
    val token: Int,
    val psm: Int,
) {
    init {
        require(psm in 1..MAX_PSM) { "PSM out of range: $psm" }
    }

    fun encode(): ByteArray = tokenBytes(token) + byteArrayOf((psm ushr 8).toByte(), psm.toByte())

    companion object {
        /** The app's service. Constant, so a receiver can tell an app phone is near (spec FR-3.4a). */
        val SERVICE_UUID: UUID = UUID.fromString("6f1c0e2a-7b3d-4e59-9a61-2d8b5c4e7a10")

        const val SIZE = 6
        private const val MAX_PSM = 0xFFFF

        /** The 4 token bytes, also the hello a connecting phone sends first on a new link. */
        fun tokenBytes(token: Int): ByteArray =
            byteArrayOf((token ushr 24).toByte(), (token ushr 16).toByte(), (token ushr 8).toByte(), token.toByte())

        /** The token in the first 4 bytes of [bytes]. */
        fun tokenOf(bytes: ByteArray): Int =
            (bytes[0].toInt() and 0xFF shl 24) or (bytes[1].toInt() and 0xFF shl 16) or
                (bytes[2].toInt() and 0xFF shl 8) or (bytes[3].toInt() and 0xFF)

        /** Null for anything that is not exactly an advertisement; a hostile phone can send anything. */
        fun decode(bytes: ByteArray): Advertisement? {
            if (bytes.size != SIZE) return null
            val psm = (bytes[4].toInt() and 0xFF shl 8) or (bytes[5].toInt() and 0xFF)
            return if (psm == 0) null else Advertisement(tokenOf(bytes), psm)
        }
    }
}
