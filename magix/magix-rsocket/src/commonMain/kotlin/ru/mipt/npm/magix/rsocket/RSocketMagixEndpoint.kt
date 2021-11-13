package ru.mipt.npm.magix.rsocket

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.core.RSocketConnector
import io.rsocket.kotlin.core.RSocketConnectorBuilder
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import io.rsocket.kotlin.transport.ktor.client.RSocketSupport
import io.rsocket.kotlin.transport.ktor.client.rSocket
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import ru.mipt.npm.magix.api.MagixEndpoint
import ru.mipt.npm.magix.api.MagixMessage
import ru.mipt.npm.magix.api.MagixMessageFilter
import ru.mipt.npm.magix.api.filter
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

public class RSocketMagixEndpoint<T>(
    payloadSerializer: KSerializer<T>,
    private val rSocket: RSocket,
    private val coroutineContext: CoroutineContext,
) : MagixEndpoint<T> {

    private val serializer = MagixMessage.serializer(payloadSerializer)

    override fun subscribe(
        filter: MagixMessageFilter,
    ): Flow<MagixMessage<T>> {
        val payload = buildPayload { data(MagixEndpoint.magixJson.encodeToString(filter)) }
        val flow = rSocket.requestStream(payload)
        return flow.map {
            MagixEndpoint.magixJson.decodeFromString(serializer, it.data.readText())
        }.filter(filter).flowOn(coroutineContext[CoroutineDispatcher] ?: Dispatchers.Unconfined)
    }

    override suspend fun broadcast(message: MagixMessage<T>) {
        withContext(coroutineContext) {
            val payload = buildPayload { data(MagixEndpoint.magixJson.encodeToString(serializer, message)) }
            rSocket.fireAndForget(payload)
        }
    }

    public companion object
}


internal fun buildConnector(rSocketConfig: RSocketConnectorBuilder.ConnectionConfigBuilder.() -> Unit) =
    RSocketConnector {
        reconnectable(10)
        connectionConfig(rSocketConfig)
    }

/**
 * Build a websocket based endpoint connected to [host], [port] and given routing [path]
 */
public suspend fun <T> MagixEndpoint.Companion.rSocketWithWebSockets(
    host: String,
    payloadSerializer: KSerializer<T>,
    port: Int = DEFAULT_MAGIX_HTTP_PORT,
    path: String = "/rsocket",
    rSocketConfig: RSocketConnectorBuilder.ConnectionConfigBuilder.() -> Unit = {},
): RSocketMagixEndpoint<T> {
    val client = HttpClient {
        install(WebSockets)
        install(RSocketSupport) {
            connector = buildConnector(rSocketConfig)
        }
    }

    val rSocket = client.rSocket(host, port, path)

    //Ensure client is closed after rSocket if finished
    rSocket.job.invokeOnCompletion {
        client.close()
    }

    return RSocketMagixEndpoint(payloadSerializer, rSocket, coroutineContext)
}