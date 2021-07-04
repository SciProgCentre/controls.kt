package ru.mipt.npm.controls.client

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.mipt.npm.controls.api.DeviceMessage
import ru.mipt.npm.controls.controllers.DeviceManager
import ru.mipt.npm.controls.controllers.respondMessage
import ru.mipt.npm.magix.api.MagixEndpoint
import ru.mipt.npm.magix.api.MagixMessage
import space.kscience.dataforge.context.error
import space.kscience.dataforge.context.logger


public const val DATAFORGE_MAGIX_FORMAT: String = "dataforge"

internal fun generateId(request: MagixMessage<*>): String = if (request.id != null) {
    "${request.id}.response"
} else {
    "df[${request.payload.hashCode()}"
}

/**
 * Communicate with server in [Magix format](https://github.com/waltz-controls/rfc/tree/master/1)
 */
public fun DeviceManager.launchDfMagix(
    endpoint: MagixEndpoint<DeviceMessage>,
    endpointID: String = DATAFORGE_MAGIX_FORMAT,
): Job = context.launch {
    endpoint.subscribe().onEach { request ->
        val responsePayload = respondMessage(request.payload)
        val response = MagixMessage(
            format = DATAFORGE_MAGIX_FORMAT,
            id = generateId(request),
            parentId = request.id,
            origin = endpointID,
            payload = responsePayload
        )
        endpoint.broadcast(response)
    }.catch { error ->
        logger.error(error) { "Error while responding to message" }
    }.launchIn(this)

    controller.messageOutput().onEach { payload ->
        endpoint.broadcast(
            MagixMessage(
                format = DATAFORGE_MAGIX_FORMAT,
                id = "df[${payload.hashCode()}]",
                origin = endpointID,
                payload = payload
            )
        )
    }.catch { error ->
        logger.error(error) { "Error while sending a message" }
    }.launchIn(this)
}

