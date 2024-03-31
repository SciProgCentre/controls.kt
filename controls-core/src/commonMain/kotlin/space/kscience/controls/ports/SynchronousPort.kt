package space.kscience.controls.ports

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextAware

/**
 * A port handler for synchronous (request-response) communication with a port.
 * Only one request could be active at a time (others are suspended).
 */
public interface SynchronousPort : ContextAware, AutoCloseable {

    public fun open()

    public val isOpen: Boolean

    /**
     * Send a single message and wait for the flow of response chunks.
     * The consumer is responsible for calling a terminal operation on the flow.
     */
    public suspend fun <R> respond(
        request: ByteArray,
        transform: suspend Flow<ByteArray>.() -> R,
    ): R
}

private class SynchronousOverAsynchronousPort(
    val port: AsynchronousPort,
    val mutex: Mutex,
) : SynchronousPort {

    override val context: Context get() = port.context

    override fun open() {
        if (!port.isOpen) port.open()
    }

    override val isOpen: Boolean get() = port.isOpen

    override fun close() {
        if (port.isOpen) port.close()
    }

    override suspend fun <R> respond(
        request: ByteArray,
        transform: suspend Flow<ByteArray>.() -> R,
    ): R = mutex.withLock {
        port.send(request)
        transform(port.subscribe())
    }
}


/**
 * Provide a synchronous wrapper for an asynchronous port.
 * Optionally provide external [mutex] for operation synchronization.
 *
 * If the [AsynchronousPort] is called directly, it could violate [SynchronousPort] contract
 * of only one request running simultaneously.
 */
public fun AsynchronousPort.asSynchronousPort(mutex: Mutex = Mutex()): SynchronousPort =
    SynchronousOverAsynchronousPort(this, mutex)

/**
 * Send request and read incoming data blocks until the delimiter is encountered
 */
public suspend fun SynchronousPort.respondWithDelimiter(
    data: ByteArray,
    delimiter: ByteArray,
): ByteArray = respond(data) {
    withDelimiter(delimiter).first()
}

public suspend fun SynchronousPort.respondStringWithDelimiter(
    data: String,
    delimiter: String,
): String = respond(data.encodeToByteArray()) {
    withStringDelimiter(delimiter).first()
}