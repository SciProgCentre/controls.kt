package space.kscience.controls.constructor

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import space.kscience.controls.api.Device
import space.kscience.controls.api.PropertyDescriptor
import space.kscience.controls.manager.ClockManager
import space.kscience.controls.spec.DevicePropertySpec
import space.kscience.controls.spec.MutableDevicePropertySpec
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Factory
import space.kscience.dataforge.context.request
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaConverter
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration

/**
 * A base for strongly typed device constructor block. Has additional delegates for type-safe devices
 */
public abstract class DeviceConstructor(
    context: Context,
    meta: Meta = Meta.EMPTY,
) : DeviceGroup(context, meta) {
    private val _bindings: MutableList<ConstructorBinding> = mutableListOf()
    public val bindings: List<ConstructorBinding> get() = _bindings

    public fun registerBinding(binding: ConstructorBinding) {
        _bindings.add(binding)
    }

    override fun registerProperty(descriptor: PropertyDescriptor, state: DeviceState<*>) {
        super.registerProperty(descriptor, state)
        registerBinding(PropertyBinding(this, descriptor.name, state))
    }

    /**
     * Create and register a timer. Timer is not counted as a device property.
     */
    public fun timer(tick: Duration): TimerState = TimerState(context.request(ClockManager), tick)
        .also { registerBinding(StateBinding(it)) }

    /**
     * Launch action that is performed on each [DeviceState] value change.
     *
     * Optionally provide [writes] - a set of states that this change affects.
     */
    public fun <T> DeviceState<T>.onChange(
        vararg writes: DeviceState<*>,
        reads: Collection<DeviceState<*>>,
        onChange: suspend (T) -> Unit,
    ): Job = valueFlow.onEach(onChange).launchIn(this@DeviceConstructor).also {
        registerBinding(ActionBinding(setOf(this, *reads.toTypedArray()), setOf(*writes)))
    }
}

/**
 * Register a device, provided by a given [factory] and
 */
public fun <D : Device> DeviceConstructor.device(
    factory: Factory<D>,
    meta: Meta? = null,
    nameOverride: Name? = null,
    metaLocation: Name? = null,
): PropertyDelegateProvider<DeviceConstructor, ReadOnlyProperty<DeviceConstructor, D>> =
    PropertyDelegateProvider { _: DeviceConstructor, property: KProperty<*> ->
        val name = nameOverride ?: property.name.asName()
        val device = install(name, factory, meta, metaLocation ?: name)
        ReadOnlyProperty { _: DeviceConstructor, _ ->
            device
        }
    }

public fun <D : Device> DeviceConstructor.device(
    device: D,
    nameOverride: Name? = null,
): PropertyDelegateProvider<DeviceConstructor, ReadOnlyProperty<DeviceConstructor, D>> =
    PropertyDelegateProvider { _: DeviceConstructor, property: KProperty<*> ->
        val name = nameOverride ?: property.name.asName()
        install(name, device)
        ReadOnlyProperty { _: DeviceConstructor, _ ->
            device
        }
    }

/**
 * Register a property and provide a direct reader for it
 */
public fun <T, S : DeviceState<T>> DeviceConstructor.property(
    state: S,
    descriptorBuilder: PropertyDescriptor.() -> Unit = {},
    nameOverride: String? = null,
): PropertyDelegateProvider<DeviceConstructor, ReadOnlyProperty<DeviceConstructor, S>> =
    PropertyDelegateProvider { _: DeviceConstructor, property ->
        val name = nameOverride ?: property.name
        val descriptor = PropertyDescriptor(name).apply(descriptorBuilder)
        registerProperty(descriptor, state)
        ReadOnlyProperty { _: DeviceConstructor, _ ->
            state
        }
    }

/**
 * Register external state as a property
 */
public fun <T : Any> DeviceConstructor.property(
    metaConverter: MetaConverter<T>,
    reader: suspend () -> T,
    readInterval: Duration,
    initialState: T,
    descriptorBuilder: PropertyDescriptor.() -> Unit = {},
    nameOverride: String? = null,
): PropertyDelegateProvider<DeviceConstructor, ReadOnlyProperty<DeviceConstructor, DeviceState<T>>> = property(
    DeviceState.external(this, metaConverter, readInterval, initialState, reader),
    descriptorBuilder,
    nameOverride,
)

/**
 * Register a mutable external state as a property
 */
public fun <T : Any> DeviceConstructor.mutableProperty(
    metaConverter: MetaConverter<T>,
    reader: suspend () -> T,
    writer: suspend (T) -> Unit,
    readInterval: Duration,
    initialState: T,
    descriptorBuilder: PropertyDescriptor.() -> Unit = {},
    nameOverride: String? = null,
): PropertyDelegateProvider<DeviceConstructor, ReadOnlyProperty<DeviceConstructor, MutableDeviceState<T>>> = property(
    DeviceState.external(this, metaConverter, readInterval, initialState, reader, writer),
    descriptorBuilder,
    nameOverride,
)

/**
 * Create and register a virtual mutable property with optional [callback]
 */
public fun <T> DeviceConstructor.virtualProperty(
    metaConverter: MetaConverter<T>,
    initialState: T,
    descriptorBuilder: PropertyDescriptor.() -> Unit = {},
    nameOverride: String? = null,
    callback: (T) -> Unit = {},
): PropertyDelegateProvider<DeviceConstructor, ReadOnlyProperty<DeviceConstructor, MutableDeviceState<T>>> = property(
    DeviceState.internal(metaConverter, initialState, callback),
    descriptorBuilder,
    nameOverride,
)

/**
 * Bind existing property provided by specification to this device
 */
public fun <T, D : Device> DeviceConstructor.deviceProperty(
    device: D,
    property: DevicePropertySpec<D, T>,
    initialValue: T,
): PropertyDelegateProvider<DeviceConstructor, ReadOnlyProperty<DeviceConstructor, DeviceState<T>>> =
    property(device.propertyAsState(property, initialValue))

/**
 * Bind existing property provided by specification to this device
 */
public fun <T, D : Device> DeviceConstructor.deviceProperty(
    device: D,
    property: MutableDevicePropertySpec<D, T>,
    initialValue: T,
): PropertyDelegateProvider<DeviceConstructor, ReadOnlyProperty<DeviceConstructor, MutableDeviceState<T>>> =
    property(device.mutablePropertyAsState(property, initialValue))