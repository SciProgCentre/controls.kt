package space.kscience.controls.manager

import kotlinx.coroutines.launch
import space.kscience.controls.api.Device
import space.kscience.controls.api.DeviceHub
import space.kscience.controls.api.id
import space.kscience.dataforge.context.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.get
import space.kscience.dataforge.names.parseAsName
import kotlin.collections.set
import kotlin.properties.ReadOnlyProperty

/**
 * DataForge Context plugin that allows to manage devices locally
 */
public class DeviceManager : AbstractPlugin(), DeviceHub {
    override val tag: PluginTag get() = Companion.tag

    /**
     * Actual list of connected devices
     */
    private val _devices = HashMap<Name, Device>()
    override val devices: Map<Name, Device> get() = _devices

    public fun registerDevice(name: Name, device: Device) {
        _devices[name] = device
    }

    override fun content(target: String): Map<Name, Any> = super<DeviceHub>.content(target)

    public companion object : PluginFactory<DeviceManager> {
        override val tag: PluginTag = PluginTag("devices", group = PluginTag.DATAFORGE_GROUP)

        override fun build(context: Context, meta: Meta): DeviceManager = DeviceManager()
    }
}

public fun <D : Device> DeviceManager.install(name: String, device: D): D {
    registerDevice(name.parseAsName(), device)
    device.launch {
        device.start()
    }
    return device
}

public fun <D : Device> DeviceManager.install(device: D): D = install(device.id, device)


public fun <D : Device> Context.install(name: String, device: D): D = request(DeviceManager).install(name, device)

public fun <D : Device> Context.install(device: D): D = request(DeviceManager).install(device.id, device)

/**
 * Register and start a device built by [factory] with current [Context] and [meta].
 */
public fun <D : Device> DeviceManager.install(name: String, factory: Factory<D>, meta: Meta = Meta.EMPTY): D =
    install(name, factory(meta, context))

/**
 * A delegate that initializes device on the first use
 */
public inline fun <D : Device> DeviceManager.installing(
    factory: Factory<D>,
    builder: MutableMeta.() -> Unit = {},
): ReadOnlyProperty<Any?, D> {
    val meta = Meta(builder)
    return ReadOnlyProperty { _, property ->
        val name = property.name
        val current = devices[name]
        if (current == null) {
            install(name, factory, meta)
        } else if (current.meta != meta) {
            error("Meta mismatch. Current device meta: ${current.meta}, but factory meta is $meta")
        } else {
            @Suppress("UNCHECKED_CAST")
            current as D
        }
    }
}