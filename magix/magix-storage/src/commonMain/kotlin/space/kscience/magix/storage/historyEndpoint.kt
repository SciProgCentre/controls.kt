package space.kscience.magix.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import space.kscience.magix.api.MagixEndpoint
import space.kscience.magix.api.MagixMessage
import space.kscience.magix.api.send
import space.kscience.magix.api.subscribe

internal fun generateId(request: MagixMessage): String = if (request.id != null) {
    "${request.id}.response"
} else {
    "history[${request.payload.hashCode().toString(16)}"
}

/**
 * Launch responding history messages on this [MagixEndpoint]. The process does not store messages, only responds to history requests.
 *
 * @param scope the [CoroutineScope] in which the responding process runs.
 * @param history the history database.
 * @param endpointName the name of this endpoint that is used as a filter.
 * @param pageSize maximum messages per page in the response. The default is 100.
 * @param user user block for outgoing messages if defined.
 * @param origin tag for outgoing messages if defined.
 */
public fun MagixEndpoint.launchHistory(
    scope: CoroutineScope,
    history: MagixHistory,
    endpointName: String? = null,
    pageSize: Int = 100,
    user: JsonElement? = endpointName?.let { JsonPrimitive(endpointName) },
    origin: String = MagixHistory.HISTORY_PAYLOAD_FORMAT,
): Job = subscribe(
    MagixHistory.magixFormat,
    targetFilter = endpointName?.let { setOf(it) }
).onEach { (request, payload) ->

    fun send(chunk: List<MagixMessage>, pageNumber: Int, end: Boolean) {
        scope.launch {
            val sendPayload = HistoryResponsePayload(
                chunk,
                pageNumber,
                lastPage = end
            )
            send(
                format = MagixHistory.magixFormat,
                payload = sendPayload,
                source = origin,
                target = request.sourceEndpoint,
                id = generateId(request),
                parentId = request.id,
                user = user,
            )
        }
    }


    if (payload is HistoryRequestPayload) {
        val realPageSize = payload.pageSize ?: pageSize
        history.useMessages(payload.magixFilter, payload.payloadFilter, payload.userFilter) { sequence ->
            // start from -1 because increment always happens first
            var pageNumber = -1

            //remember the last chunk to determine which is last
            var chunk: List<MagixMessage>? = null

            sequence.chunked(realPageSize).forEach {
                //If the last chunk was not final, send it
                chunk?.let { chunk ->
                    send(chunk, pageNumber, false)
                }
                pageNumber++
                // update last chunk
                chunk = it
            }
            // send the final chunk
            chunk?.let {
                send(it, pageNumber, true)
            }
        }
    }
}.launchIn(scope)