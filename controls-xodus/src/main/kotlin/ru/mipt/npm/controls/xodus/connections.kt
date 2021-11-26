package ru.mipt.npm.controls.xodus

import io.ktor.application.*
import io.ktor.server.engine.*
import jetbrains.exodus.entitystore.PersistentEntityStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.mipt.npm.controls.api.DeviceMessage
import ru.mipt.npm.controls.api.PropertyChangedMessage
import ru.mipt.npm.controls.controllers.DeviceManager
import ru.mipt.npm.controls.controllers.hubMessageFlow
import ru.mipt.npm.magix.api.MagixEndpoint
import ru.mipt.npm.magix.server.GenericMagixMessage
import ru.mipt.npm.magix.server.startMagixServer


public fun DeviceManager.connectXodus(
    entityStore: PersistentEntityStore,
    filterCondition: suspend (DeviceMessage) -> Boolean  = { it is PropertyChangedMessage }
): Job = hubMessageFlow(context).filter(filterCondition).onEach { message ->
    entityStore.executeInTransaction {
        (message as PropertyChangedMessage).toEntity(it)
    }
}.launchIn(context)

public fun CoroutineScope.startMagixServer(
    entityStore: PersistentEntityStore,
    flowFilter: suspend (GenericMagixMessage) -> Boolean = { true },
    port: Int = MagixEndpoint.DEFAULT_MAGIX_HTTP_PORT,
    buffer: Int = 100,
    enableRawRSocket: Boolean = true,
    enableZmq: Boolean = true,
    applicationConfiguration: Application.(MutableSharedFlow<GenericMagixMessage>) -> Unit = {},
): ApplicationEngine = startMagixServer(
    port, buffer, enableRawRSocket, enableZmq
) { flow ->
    applicationConfiguration(flow)
    flow.filter(flowFilter).onEach { message ->
        entityStore.executeInTransaction { txn ->
            val entity = txn.newEntity("MagixMessage")
            entity.setProperty("value", message.toString())
        }
    }
}
