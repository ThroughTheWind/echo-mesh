package io.github.throughthewind.echo.adapters.ble

import io.github.throughthewind.echo.core.mesh.Unavailable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import java.io.IOException

/** The air between [FakeBleRadio]s: who advertises what, and who listens on which PSM. */
class FakeAir {
    internal val ads = MutableStateFlow<Map<FakeBleRadio, ByteArray>>(emptyMap())
    internal val listeners = mutableMapOf<Pair<FakeBleRadio, Int>, Channel<Link>>()
    private var nextPsm = 0x80

    /** Every advertisement ever put on the air, in order, to check what a phone gave away. */
    val advertised = mutableListOf<ByteArray>()

    internal fun psm() = nextPsm++

    fun radio() = FakeBleRadio(this)
}

/** A [BleRadio] in memory: advertisements are heard at once by every other radio on the same air. */
class FakeBleRadio internal constructor(
    private val air: FakeAir,
) : BleRadio {
    var problem: Unavailable? = null

    /** The next this many [listen] calls fail, as with Bluetooth turned off under the app. */
    var listenFailures = 0

    override fun unavailable(): Unavailable? = problem

    override suspend fun advertise(payload: ByteArray) {
        air.advertised += payload.copyOf()
        air.ads.value = air.ads.value + (this to payload.copyOf())
    }

    override suspend fun stopAdvertising() {
        air.ads.value = air.ads.value - this
    }

    override fun sightings(): Flow<Sighting> =
        flow {
            air.ads.collect { ads ->
                ads.forEach { (radio, payload) -> if (radio !== this@FakeBleRadio) emit(Sighting(payload, radio)) }
            }
        }

    override suspend fun listen(): Listener {
        if (listenFailures > 0) {
            listenFailures--
            throw IOException("Bluetooth is off")
        }
        val psm = air.psm()
        val incoming = Channel<Link>(Channel.UNLIMITED)
        air.listeners[this to psm] = incoming
        return object : Listener {
            override val psm = psm

            override suspend fun accept(): Link =
                try {
                    incoming.receive()
                } catch (_: ClosedReceiveChannelException) {
                    throw IOException("listener closed")
                }

            override fun close() {
                air.listeners.remove(this@FakeBleRadio to psm)
                incoming.close()
            }
        }
    }

    override suspend fun connect(
        sighting: Sighting,
        psm: Int,
    ): Link {
        val target = sighting.target as FakeBleRadio
        val incoming = air.listeners[target to psm] ?: throw IOException("nobody listens on $psm")
        val ab = Channel<ByteArray>(Channel.UNLIMITED)
        val ba = Channel<ByteArray>(Channel.UNLIMITED)
        val mine = FakeLink(inbound = ba, outbound = ab)
        val theirs = FakeLink(inbound = ab, outbound = ba)
        mine.other = theirs
        theirs.other = mine
        incoming.send(theirs)
        return mine
    }
}

/** One end of an in-memory L2CAP channel. */
class FakeLink internal constructor(
    private val inbound: Channel<ByteArray>,
    private val outbound: Channel<ByteArray>,
) : Link {
    internal var other: FakeLink? = null
    private var pending = ByteArray(0)

    override suspend fun read(buffer: ByteArray): Int {
        if (pending.isEmpty()) {
            pending =
                try {
                    inbound.receive()
                } catch (_: ClosedReceiveChannelException) {
                    return -1
                }
        }
        val n = minOf(buffer.size, pending.size)
        pending.copyInto(buffer, 0, 0, n)
        pending = pending.copyOfRange(n, pending.size)
        return n
    }

    override suspend fun write(bytes: ByteArray) {
        try {
            outbound.send(bytes.copyOf())
        } catch (_: ClosedSendChannelException) {
            throw IOException("link closed")
        }
    }

    /** Raw bytes, as a hostile phone would write them. */
    suspend fun writeRaw(bytes: ByteArray) = write(bytes)

    override fun close() {
        outbound.close()
        inbound.close()
    }
}
