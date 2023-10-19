package space.kscience.controls.client

import com.benasher44.uuid.uuid4
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.newCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.kscience.controls.api.*
import space.kscience.controls.manager.DeviceManager
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.magix.api.MagixEndpoint
import space.kscience.magix.api.send
import space.kscience.magix.api.subscribe
import kotlin.coroutines.CoroutineContext

private fun stringUID() = uuid4().leastSignificantBits.toString(16)

/**
 * A remote accessible device that relies on connection via Magix
 */
public class DeviceClient(
    override val context: Context,
    private val deviceName: Name,
    incomingFlow: Flow<DeviceMessage>,
    private val send: suspend (DeviceMessage) -> Unit,
) : Device {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val coroutineContext: CoroutineContext = newCoroutineContext(context.coroutineContext)

    private val mutex = Mutex()

    private val propertyCache = HashMap<String, Meta>()

    override var propertyDescriptors: Collection<PropertyDescriptor> = emptyList()
        private set

    override var actionDescriptors: Collection<ActionDescriptor> = emptyList()
        private set

    private val flowInternal = incomingFlow.filter {
        it.sourceDevice == deviceName
    }.shareIn(this, started = SharingStarted.Eagerly).also {
        it.onEach { message ->
            when (message) {
                is PropertyChangedMessage -> mutex.withLock {
                    propertyCache[message.property] = message.value
                }

                is DescriptionMessage -> mutex.withLock {
                    propertyDescriptors = message.properties
                    actionDescriptors = message.actions
                }

                else -> {
                    //ignore
                }
            }
        }.launchIn(this)
    }

    override val messageFlow: Flow<DeviceMessage> get() = flowInternal


    override suspend fun readProperty(propertyName: String): Meta {
        send(
            PropertyGetMessage(propertyName, targetDevice = deviceName)
        )
        return flowInternal.filterIsInstance<PropertyChangedMessage>().first {
            it.property == propertyName
        }.value
    }

    override fun getProperty(propertyName: String): Meta? = propertyCache[propertyName]

    override suspend fun invalidate(propertyName: String) {
        mutex.withLock {
            propertyCache.remove(propertyName)
        }
    }

    override suspend fun writeProperty(propertyName: String, value: Meta) {
        send(
            PropertySetMessage(propertyName, value, targetDevice = deviceName)
        )
    }

    override suspend fun execute(actionName: String, argument: Meta?): Meta? {
        val id = stringUID()
        send(
            ActionExecuteMessage(actionName, argument, id, targetDevice = deviceName)
        )
        return flowInternal.filterIsInstance<ActionResultMessage>().first {
            it.action == actionName && it.requestId == id
        }.result
    }

    @DFExperimental
    override val lifecycleState: DeviceLifecycleState = DeviceLifecycleState.OPEN
}

/**
 * Connect to a remote device via this endpoint.
 *
 * @param context a [Context] to run device in
 * @param endpointName the name of endpoint in Magix to connect to
 * @param deviceName the name of device within endpoint
 */
public fun MagixEndpoint.remoteDevice(context: Context, endpointName: String, deviceName: Name): DeviceClient {
    val subscription = subscribe(DeviceManager.magixFormat, originFilter = listOf(endpointName)).map { it.second }
    return DeviceClient(context, deviceName, subscription) {
        send(DeviceManager.magixFormat, it, endpointName, id = stringUID())
    }
}