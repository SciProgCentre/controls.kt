package ru.mipt.npm.controls.spec

import kotlinx.coroutines.withContext
import ru.mipt.npm.controls.api.ActionDescriptor
import ru.mipt.npm.controls.api.Device
import ru.mipt.npm.controls.api.PropertyDescriptor
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.transformations.MetaConverter
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

@OptIn(InternalDeviceAPI::class)
public abstract class DeviceSpec<D : Device> {
    private val _properties = HashMap<String, DevicePropertySpec<D, *>>()
    public val properties: Map<String, DevicePropertySpec<D, *>> get() = _properties

    private val _actions = HashMap<String, DeviceActionSpec<D, *, *>>()
    public val actions: Map<String, DeviceActionSpec<D, *, *>> get() = _actions

    public fun <T : Any, P : DevicePropertySpec<D, T>> registerProperty(deviceProperty: P): P {
        _properties[deviceProperty.name] = deviceProperty
        return deviceProperty
    }

    public fun <T : Any> registerProperty(
        converter: MetaConverter<T>,
        readOnlyProperty: KProperty1<D, T>,
        descriptorBuilder: PropertyDescriptor.() -> Unit = {}
    ): DevicePropertySpec<D, T> {
        val deviceProperty = object : DevicePropertySpec<D, T> {
            override val name: String = readOnlyProperty.name
            override val descriptor: PropertyDescriptor = PropertyDescriptor(this.name).apply(descriptorBuilder)
            override val converter: MetaConverter<T> = converter
            override suspend fun read(device: D): T =
                withContext(device.coroutineContext) { readOnlyProperty.get(device) }
        }
        return registerProperty(deviceProperty)
    }

    public fun <T : Any> property(
        converter: MetaConverter<T>,
        readWriteProperty: KMutableProperty1<D, T>,
        descriptorBuilder: PropertyDescriptor.() -> Unit = {}
    ): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<Any?, WritableDevicePropertySpec<D, T>>> =
        PropertyDelegateProvider { _, property ->
            val deviceProperty = object : WritableDevicePropertySpec<D, T> {
                override val name: String = property.name

                override val descriptor: PropertyDescriptor = PropertyDescriptor(name).apply {
                    //TODO add type from converter
                    writable = true
                }.apply(descriptorBuilder)

                override val converter: MetaConverter<T> = converter

                override suspend fun read(device: D): T = withContext(device.coroutineContext) {
                    readWriteProperty.get(device)
                }

                override suspend fun write(device: D, value: T): Unit = withContext(device.coroutineContext) {
                    readWriteProperty.set(device, value)
                }
            }
            registerProperty(deviceProperty)
            ReadOnlyProperty { _, _ ->
                deviceProperty
            }
        }

    public fun <T : Any> property(
        converter: MetaConverter<T>,
        descriptorBuilder: PropertyDescriptor.() -> Unit = {},
        name: String? = null,
        read: suspend D.() -> T
    ): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, DevicePropertySpec<D, T>>> =
        PropertyDelegateProvider { _: DeviceSpec<D>, property ->
            val propertyName = name ?: property.name
            val deviceProperty = object : DevicePropertySpec<D, T> {
                override val name: String = propertyName
                override val descriptor: PropertyDescriptor = PropertyDescriptor(this.name).apply(descriptorBuilder)
                override val converter: MetaConverter<T> = converter

                override suspend fun read(device: D): T = withContext(device.coroutineContext) { device.read() }
            }
            registerProperty(deviceProperty)
            ReadOnlyProperty<DeviceSpec<D>, DevicePropertySpec<D, T>> { _, _ ->
                deviceProperty
            }
        }

    public fun <T : Any> property(
        converter: MetaConverter<T>,
        descriptorBuilder: PropertyDescriptor.() -> Unit = {},
        name: String? = null,
        read: suspend D.() -> T,
        write: suspend D.(T) -> Unit
    ): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, WritableDevicePropertySpec<D, T>>> =
        PropertyDelegateProvider { _: DeviceSpec<D>, property: KProperty<*> ->
            val propertyName = name ?: property.name
            val deviceProperty = object : WritableDevicePropertySpec<D, T> {
                override val name: String = propertyName
                override val descriptor: PropertyDescriptor = PropertyDescriptor(this.name).apply(descriptorBuilder)
                override val converter: MetaConverter<T> = converter

                override suspend fun read(device: D): T = withContext(device.coroutineContext) { device.read() }

                override suspend fun write(device: D, value: T): Unit = withContext(device.coroutineContext) {
                    device.write(value)
                }
            }
            _properties[propertyName] = deviceProperty
            ReadOnlyProperty<DeviceSpec<D>, WritableDevicePropertySpec<D, T>> { _, _ ->
                deviceProperty
            }
        }


    public fun <I : Any, O : Any> registerAction(deviceAction: DeviceActionSpec<D, I, O>): DeviceActionSpec<D, I, O> {
        _actions[deviceAction.name] = deviceAction
        return deviceAction
    }

    public fun <I : Any, O : Any> action(
        inputConverter: MetaConverter<I>,
        outputConverter: MetaConverter<O>,
        descriptorBuilder: ActionDescriptor.() -> Unit = {},
        name: String? = null,
        execute: suspend D.(I?) -> O?
    ): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, DeviceActionSpec<D, I, O>>> =
        PropertyDelegateProvider { _: DeviceSpec<D>, property ->
            val actionName = name ?: property.name
            val deviceAction = object : DeviceActionSpec<D, I, O> {
                override val name: String = actionName
                override val descriptor: ActionDescriptor = ActionDescriptor(actionName).apply(descriptorBuilder)

                override val inputConverter: MetaConverter<I> = inputConverter
                override val outputConverter: MetaConverter<O> = outputConverter

                override suspend fun execute(device: D, input: I?): O? = withContext(device.coroutineContext) {
                    device.execute(input)
                }
            }
            _actions[actionName] = deviceAction
            ReadOnlyProperty<DeviceSpec<D>, DeviceActionSpec<D, I, O>> { _, _ ->
                deviceAction
            }
        }

    /**
     * An action that takes [Meta] and returns [Meta]. No conversions are done
     */
    public fun metaAction(
        descriptorBuilder: ActionDescriptor.() -> Unit = {},
        name: String? = null,
        execute: suspend D.(Meta?) -> Meta?
    ): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, DeviceActionSpec<D, Meta, Meta>>> = action(
        MetaConverter.Companion.meta,
        MetaConverter.Companion.meta,
        descriptorBuilder,
        name
    ){
        execute(it)
    }

    /**
     * An action that takes no parameters and returns no values
     */
    public fun unitAction(
        descriptorBuilder: ActionDescriptor.() -> Unit = {},
        name: String? = null,
        execute: suspend D.() -> Unit
    ): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, DeviceActionSpec<D, Meta, Meta>>> = action(
        MetaConverter.Companion.meta,
        MetaConverter.Companion.meta,
        descriptorBuilder,
        name
    ){
        execute()
        null
    }
}