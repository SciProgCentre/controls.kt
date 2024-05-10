package space.kscience.controls.constructor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaConverter
import kotlin.reflect.KProperty

/**
 * An observable state of a device
 */
public interface DeviceState<T> {
    public val converter: MetaConverter<T>
    public val value: T

    public val valueFlow: Flow<T>

    override fun toString(): String

    public companion object
}

public val <T> DeviceState<T>.metaFlow: Flow<Meta> get() = valueFlow.map(converter::convert)

public val <T> DeviceState<T>.valueAsMeta: Meta get() = converter.convert(value)

public operator fun <T> DeviceState<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

/**
 * Collect values in a given [scope]
 */
public fun <T> DeviceState<T>.collectValuesIn(scope: CoroutineScope, block: suspend (T) -> Unit): Job =
    valueFlow.onEach(block).launchIn(scope)

/**
 * A mutable state of a device
 */
public interface MutableDeviceState<T> : DeviceState<T> {
    override var value: T
}

public operator fun <T> MutableDeviceState<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this.value = value
}

public var <T> MutableDeviceState<T>.valueAsMeta: Meta
    get() = converter.convert(value)
    set(arg) {
        value = converter.read(arg)
    }

/**
 * Device state with a value that depends on other device states
 */
public interface DeviceStateWithDependencies<T> : DeviceState<T> {
    public val dependencies: Collection<DeviceState<*>>
}

/**
 * Create a new read-only [DeviceState] that mirrors receiver state by mapping the value with [mapper].
 */
public fun <T, R> DeviceState<T>.map(
    converter: MetaConverter<R>, mapper: (T) -> R,
): DeviceStateWithDependencies<R> = object : DeviceStateWithDependencies<R> {
    override val dependencies = listOf(this)

    override val converter: MetaConverter<R> = converter

    override val value: R
        get() = mapper(this@map.value)

    override val valueFlow: Flow<R> = this@map.valueFlow.map(mapper)

    override fun toString(): String = "DeviceState.map(arg=${this@map}, converter=$converter)"
}

/**
 * Combine two device states into one read-only [DeviceState]. Only the latest value of each state is used.
 */
public fun <T1, T2, R> combine(
    state1: DeviceState<T1>,
    state2: DeviceState<T2>,
    converter: MetaConverter<R>,
    mapper: (T1, T2) -> R,
): DeviceStateWithDependencies<R> = object : DeviceStateWithDependencies<R> {
    override val dependencies = listOf(state1, state2)

    override val converter: MetaConverter<R> = converter

    override val value: R get() = mapper(state1.value, state2.value)

    override val valueFlow: Flow<R> = kotlinx.coroutines.flow.combine(state1.valueFlow, state2.valueFlow, mapper)

    override fun toString(): String = "DeviceState.combine(state1=$state1, state2=$state2)"
}
