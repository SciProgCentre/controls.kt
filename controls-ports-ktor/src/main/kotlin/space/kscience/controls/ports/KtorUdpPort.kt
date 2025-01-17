package space.kscience.controls.ports

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.consumeEachBufferRange
import io.ktor.utils.io.core.Closeable
import io.ktor.utils.io.writeAvailable
import kotlinx.coroutines.*
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.int
import space.kscience.dataforge.meta.number
import space.kscience.dataforge.meta.string
import kotlin.coroutines.CoroutineContext

public class KtorUdpPort internal constructor(
    context: Context,
    public val remoteHost: String,
    public val remotePort: Int,
    public val localPort: Int? = null,
    public val localHost: String = "localhost",
    coroutineContext: CoroutineContext = context.coroutineContext,
    socketOptions: SocketOptions.UDPSocketOptions.() -> Unit = {}
) : AbstractPort(context, coroutineContext), Closeable {

    override fun toString(): String = "port[udp:$remoteHost:$remotePort]"

    private val futureSocket = scope.async {
        aSocket(ActorSelectorManager(Dispatchers.IO)).udp().connect(
            remoteAddress = InetSocketAddress(remoteHost, remotePort),
            localAddress = localPort?.let { InetSocketAddress(localHost, localPort) },
            configure = socketOptions
        )
    }

    private val writeChannel: Deferred<ByteWriteChannel> = scope.async {
        futureSocket.await().openWriteChannel(true)
    }

    private val listenerJob = scope.launch {
        val input = futureSocket.await().openReadChannel()
        input.consumeEachBufferRange { buffer, _ ->
            val array = ByteArray(buffer.remaining())
            buffer.get(array)
            receive(array)
            isActive
        }
    }

    override suspend fun write(data: ByteArray) {
        writeChannel.await().writeAvailable(data)
    }

    override fun close() {
        listenerJob.cancel()
        futureSocket.cancel()
        super.close()
    }

    public companion object : PortFactory {

        override val type: String = "udp"

        public fun open(
            context: Context,
            remoteHost: String,
            remotePort: Int,
            localPort: Int? = null,
            localHost: String = "localhost",
            coroutineContext: CoroutineContext = context.coroutineContext,
            socketOptions: SocketOptions.UDPSocketOptions.() -> Unit = {}
        ): KtorUdpPort = KtorUdpPort(
            context = context,
            remoteHost = remoteHost,
            remotePort = remotePort,
            localPort = localPort,
            localHost = localHost,
            coroutineContext = coroutineContext,
            socketOptions = socketOptions
        )

        override fun build(context: Context, meta: Meta): Port {
            val remoteHost by meta.string { error("Remote host is not specified") }
            val remotePort by meta.number { error("Remote port is not specified") }
            val localHost: String? by meta.string()
            val localPort: Int? by meta.int()
            return open(context, remoteHost, remotePort.toInt(), localPort, localHost ?: "localhost")
        }
    }
}