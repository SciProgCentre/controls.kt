package space.kscience.controls.constructor

import space.kscience.controls.api.Device
import space.kscience.controls.manager.ClockManager
import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.context.request
import space.kscience.dataforge.meta.MetaConverter
import kotlin.time.Duration

/**
 * A binding that is used to describe device functionality
 */
public sealed interface StateDescriptor

/**
 * A binding that exposes device property as read-only state
 */
public class PropertyStateDescriptor<T>(
    public val device: Device,
    public val propertyName: String,
    public val state: DeviceState<T>,
) : StateDescriptor

/**
 * A binding for independent state like a timer
 */
public class StateBinding<T>(
    public val state: DeviceState<T>,
) : StateDescriptor

public class ConnectionStateDescriptor(
    public val reads: Collection<DeviceState<*>>,
    public val writes: Collection<DeviceState<*>>,
) : StateDescriptor


public interface StateContainer : ContextAware {
    public val stateDescriptors: List<StateDescriptor>
    public fun registerState(stateDescriptor: StateDescriptor)
}

/**
 * Register a [state] in this container. The state is not registered as a device property if [this] is a [DeviceConstructor]
 */
public fun <T, D : DeviceState<T>> StateContainer.state(state: D): D {
    registerState(StateBinding(state))
    return state
}

/**
 * Create a register a [MutableDeviceState] with a given [converter]
 */
public fun <T> StateContainer.state(converter: MetaConverter<T>, initialValue: T): MutableDeviceState<T> = state(
    DeviceState.internal(converter, initialValue)
)

/**
 * Create a register a mutable [Double] state
 */
public fun StateContainer.doubleState(initialValue: Double): MutableDeviceState<Double> = state(
    MetaConverter.double, initialValue
)

/**
 * Create a register a mutable [String] state
 */
public fun StateContainer.stringState(initialValue: String): MutableDeviceState<String> = state(
    MetaConverter.string, initialValue
)

/**
 * Create and register a timer state.
 */
public fun StateContainer.timer(tick: Duration): TimerState = state(TimerState(context.request(ClockManager), tick))
