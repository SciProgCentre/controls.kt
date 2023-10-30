package space.kscience.controls.constructor

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import space.kscience.controls.api.Device
import space.kscience.controls.api.PropertyChangedMessage
import space.kscience.controls.spec.DevicePropertySpec
import space.kscience.controls.spec.MutableDevicePropertySpec
import space.kscience.controls.spec.name
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.transformations.MetaConverter

/**
 * An observable state of a device
 */
public interface DeviceState<T> {
    public val converter: MetaConverter<T>
    public val value: T

    public val valueFlow: Flow<T>
}

public val <T> DeviceState<T>.metaFlow: Flow<Meta> get() = valueFlow.map(converter::objectToMeta)

public val <T> DeviceState<T>.valueAsMeta: Meta get() = converter.objectToMeta(value)


/**
 * A mutable state of a device
 */
public interface MutableDeviceState<T> : DeviceState<T> {
    override var value: T
}

public var <T : Any> MutableDeviceState<T>.valueAsMeta: Meta
    get() = converter.objectToMeta(value)
    set(arg) {
        value = converter.metaToObject(arg) ?: error("Conversion for meta $arg to property type with $converter failed")
    }

/**
 * A [MutableDeviceState] that does not correspond to a physical state
 */
public class VirtualDeviceState<T>(
    override val converter: MetaConverter<T>,
    initialValue: T,
) : MutableDeviceState<T> {
    private val flow = MutableStateFlow(initialValue)
    override val valueFlow: Flow<T> get() = flow

    override var value: T by flow::value
}

private open class BoundDeviceState<T>(
    override val converter: MetaConverter<T>,
    val device: Device,
    val propertyName: String,
    private val initialValue: T,
) : DeviceState<T> {

    override val valueFlow: StateFlow<T> = device.messageFlow.filterIsInstance<PropertyChangedMessage>().filter {
        it.property == propertyName
    }.mapNotNull {
        converter.metaToObject(it.value)
    }.stateIn(device.context, SharingStarted.Eagerly, initialValue)

    override val value: T get() = valueFlow.value
}

/**
 * Bind a read-only [DeviceState] to a [Device] property
 */
public suspend fun <T> Device.bindStateToProperty(
    propertyName: String,
    metaConverter: MetaConverter<T>,
): DeviceState<T> {
    val initialValue = metaConverter.metaToObject(readProperty(propertyName)) ?: error("Conversion of property failed")
    return BoundDeviceState(metaConverter, this, propertyName, initialValue)
}

public suspend fun <D : Device, T> D.bindStateToProperty(
    propertySpec: DevicePropertySpec<D, T>,
): DeviceState<T> = bindStateToProperty(propertySpec.name, propertySpec.converter)

public fun <T, R> DeviceState<T>.map(
    converter: MetaConverter<R>, mapper: (T) -> R,
): DeviceState<R> = object : DeviceState<R> {
    override val converter: MetaConverter<R> = converter
    override val value: R
        get() = mapper(this@map.value)

    override val valueFlow: Flow<R> = this@map.valueFlow.map(mapper)
}

private class MutableBoundDeviceState<T>(
    converter: MetaConverter<T>,
    device: Device,
    propertyName: String,
    initialValue: T,
) : BoundDeviceState<T>(converter, device, propertyName, initialValue), MutableDeviceState<T> {

    override var value: T
        get() = valueFlow.value
        set(newValue) {
            device.launch {
                device.writeProperty(propertyName, converter.objectToMeta(newValue))
            }
        }
}

public suspend fun <T> Device.bindMutableStateToProperty(
    propertyName: String,
    metaConverter: MetaConverter<T>,
): MutableDeviceState<T> {
    val initialValue = metaConverter.metaToObject(readProperty(propertyName)) ?: error("Conversion of property failed")
    return MutableBoundDeviceState(metaConverter, this, propertyName, initialValue)
}

public suspend fun <D : Device, T> D.bindMutableStateToProperty(
    propertySpec: MutableDevicePropertySpec<D, T>,
): MutableDeviceState<T> = bindMutableStateToProperty(propertySpec.name, propertySpec.converter)


