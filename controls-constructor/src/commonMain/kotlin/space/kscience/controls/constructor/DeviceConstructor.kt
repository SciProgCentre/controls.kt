package space.kscience.controls.constructor

import space.kscience.controls.api.Device
import space.kscience.controls.api.PropertyDescriptor
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Factory
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaConverter
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration

/**
 * A base for strongly typed device constructor blocks. Has additional delegates for type-safe devices
 */
public abstract class DeviceConstructor(
    context: Context,
    meta: Meta,
) : DeviceGroup(context, meta) {

    /**
     * Register a device, provided by a given [factory] and
     */
    public fun <D : Device> device(
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

    public fun <D : Device> device(
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
    public fun <T : Any, S: DeviceState<T>> property(
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
    public fun <T : Any> property(
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
    public fun <T : Any> mutableProperty(
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
    public fun <T : Any> virtualProperty(
        metaConverter: MetaConverter<T>,
        initialState: T,
        descriptorBuilder: PropertyDescriptor.() -> Unit = {},
        nameOverride: String? = null,
        callback: (T) -> Unit = {},
    ): PropertyDelegateProvider<DeviceConstructor, ReadOnlyProperty<DeviceConstructor, MutableDeviceState<T>>> = property(
        DeviceState.virtual(metaConverter, initialState, callback),
        descriptorBuilder,
        nameOverride,
    )
}