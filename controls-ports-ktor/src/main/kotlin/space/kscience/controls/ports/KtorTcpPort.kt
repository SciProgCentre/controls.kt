package space.kscience.controls.ports

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.SocketOptions
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.consumeEachBufferRange
import io.ktor.utils.io.core.Closeable
import io.ktor.utils.io.writeAvailable
import kotlinx.coroutines.*
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Factory
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.int
import space.kscience.dataforge.meta.string
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

public class KtorTcpPort internal constructor(
    context: Context,
    meta: Meta,
    public val host: String,
    public val port: Int,
    coroutineContext: CoroutineContext = context.coroutineContext,
    socketOptions: SocketOptions.TCPClientSocketOptions.() -> Unit = {},
) : AbstractAsynchronousPort(context, meta, coroutineContext), Closeable {

    override fun toString(): String = "port[tcp:$host:$port]"

    private val futureSocket = scope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
        aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(host, port, socketOptions)
    }

    private val writeChannel = scope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
        futureSocket.await().openWriteChannel(true)
    }

    private var listenerJob: Job? = null

    override fun onOpen() {
        listenerJob = scope.launch {
            val input = futureSocket.await().openReadChannel()
            input.consumeEachBufferRange { buffer: ByteBuffer, last ->
                val array = ByteArray(buffer.remaining())
                buffer.get(array)
                receive(array)
                !last && isActive
            }
        }
    }

    override suspend fun write(data: ByteArray) {
        writeChannel.await().writeAvailable(data)
    }

    override val isOpen: Boolean
        get() = listenerJob?.isActive == true

    override fun close() {
        listenerJob?.cancel()
        futureSocket.cancel()
        super.close()
    }

    public companion object : Factory<AsynchronousPort> {

        public fun build(
            context: Context,
            host: String,
            port: Int,
            coroutineContext: CoroutineContext = context.coroutineContext,
            socketOptions: SocketOptions.TCPClientSocketOptions.() -> Unit = {},
        ): KtorTcpPort {
            val meta = Meta {
                "name" put "tcp://$host:$port"
                "type" put "tcp"
                "host" put host
                "port" put port
            }
            return KtorTcpPort(context, meta, host, port, coroutineContext, socketOptions)
        }

        public fun open(
            context: Context,
            host: String,
            port: Int,
            coroutineContext: CoroutineContext = context.coroutineContext,
            socketOptions: SocketOptions.TCPClientSocketOptions.() -> Unit = {},
        ): KtorTcpPort = build(context, host, port, coroutineContext, socketOptions).apply { open() }

        override fun build(context: Context, meta: Meta): AsynchronousPort {
            val host = meta["host"].string ?: "localhost"
            val port = meta["port"].int ?: error("Port value for TCP port is not defined in $meta")
            return build(context, host, port)
        }
    }
}