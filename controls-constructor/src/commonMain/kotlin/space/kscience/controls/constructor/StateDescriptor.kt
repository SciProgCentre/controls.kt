package space.kscience.controls.constructor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import space.kscience.controls.api.Device
import space.kscience.controls.manager.ClockManager
import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.context.request
import kotlin.time.Duration

/**
 * A binding that is used to describe device functionality
 */
public sealed interface StateDescriptor

/**
 * A binding that exposes device property as read-only state
 */
public class StatePropertyDescriptor<T>(
    public val device: Device,
    public val propertyName: String,
    public val state: DeviceState<T>,
) : StateDescriptor

/**
 * A binding for independent state like a timer
 */
public class StateNodeDescriptor<T>(
    public val state: DeviceState<T>,
) : StateDescriptor

public class StateConnectionDescriptor(
    public val reads: Collection<DeviceState<*>>,
    public val writes: Collection<DeviceState<*>>,
) : StateDescriptor


public interface StateContainer : ContextAware, CoroutineScope {
    public val stateDescriptors: Set<StateDescriptor>
    public fun registerState(stateDescriptor: StateDescriptor)
    public fun unregisterState(stateDescriptor: StateDescriptor)


    /**
     * Bind an action to a [DeviceState]. [onChange] block is performed on each state change
     *
     * Optionally provide [writes] - a set of states that this change affects.
     */
    public fun <T> DeviceState<T>.onNext(
        vararg writes: DeviceState<*>,
        alsoReads: Collection<DeviceState<*>> = emptySet(),
        onChange: suspend (T) -> Unit,
    ): Job = valueFlow.onEach(onChange).launchIn(this@StateContainer).also {
        registerState(StateConnectionDescriptor(setOf(this, *alsoReads.toTypedArray()), setOf(*writes)))
    }

    public fun <T> DeviceState<T>.onChange(
        vararg writes: DeviceState<*>,
        alsoReads: Collection<DeviceState<*>> = emptySet(),
        onChange: suspend (prev: T, next: T) -> Unit,
    ): Job = valueFlow.runningFold(Pair(value, value)) { pair, next ->
        Pair(pair.second, next)
    }.onEach { pair ->
        if (pair.first != pair.second) {
            onChange(pair.first, pair.second)
        }
    }.launchIn(this@StateContainer).also {
        registerState(StateConnectionDescriptor(setOf(this, *alsoReads.toTypedArray()), setOf(*writes)))
    }
}

/**
 * Register a [state] in this container. The state is not registered as a device property if [this] is a [DeviceConstructor]
 */
public fun <T, D : DeviceState<T>> StateContainer.state(state: D): D {
    registerState(StateNodeDescriptor(state))
    return state
}

/**
 * Create a register a [MutableDeviceState] with a given [converter]
 */
public fun <T> StateContainer.mutableState(initialValue: T): MutableDeviceState<T> = state(
    MutableDeviceState(initialValue)
)

public fun <T : DeviceModel> StateContainer.model(model: T): T {
    model.stateDescriptors.forEach {
        registerState(it)
    }
    return model
}

/**
 * Create and register a timer state.
 */
public fun StateContainer.timer(tick: Duration): TimerState = state(TimerState(context.request(ClockManager), tick))


public fun <T, R> StateContainer.mapState(
    state: DeviceState<T>,
    transformation: (T) -> R,
): DeviceStateWithDependencies<R> = state(DeviceState.map(state, transformation))

/**
 * Create a new state by combining two existing ones
 */
public fun <T1, T2, R> StateContainer.combineState(
    first: DeviceState<T1>,
    second: DeviceState<T2>,
    transformation: (T1, T2) -> R,
): DeviceState<R> = state(DeviceState.combine(first, second, transformation))


/**
 * Create and start binding between [sourceState] and [targetState]. Changes made to [sourceState] are automatically
 * transferred onto [targetState], but not vise versa.
 *
 * On resulting [Job] cancel the binding is unregistered
 */
public fun <T> StateContainer.bindTo(sourceState: DeviceState<T>, targetState: MutableDeviceState<T>): Job {
    val descriptor = StateConnectionDescriptor(setOf(sourceState), setOf(targetState))
    registerState(descriptor)
    return sourceState.valueFlow.onEach {
        targetState.value = it
    }.launchIn(this).apply {
        invokeOnCompletion {
            unregisterState(descriptor)
        }
    }
}

/**
 * Create and start binding between [sourceState] and [targetState]. Changes made to [sourceState] are automatically
 * transferred onto [targetState] via [transformation], but not vise versa.
 *
 * On resulting [Job] cancel the binding is unregistered
 */
public fun <T, R> StateContainer.transformTo(
    sourceState: DeviceState<T>,
    targetState: MutableDeviceState<R>,
    transformation: suspend (T) -> R,
): Job {
    val descriptor = StateConnectionDescriptor(setOf(sourceState), setOf(targetState))
    registerState(descriptor)
    return sourceState.valueFlow.onEach {
        targetState.value = transformation(it)
    }.launchIn(this).apply {
        invokeOnCompletion {
            unregisterState(descriptor)
        }
    }
}

/**
 * Register [StateDescriptor] that combines values from [sourceState1] and [sourceState2] using [transformation].
 *
 * On resulting [Job] cancel the binding is unregistered
 */
public fun <T1, T2, R> StateContainer.combineTo(
    sourceState1: DeviceState<T1>,
    sourceState2: DeviceState<T2>,
    targetState: MutableDeviceState<R>,
    transformation: suspend (T1, T2) -> R,
): Job {
    val descriptor = StateConnectionDescriptor(setOf(sourceState1, sourceState2), setOf(targetState))
    registerState(descriptor)
    return kotlinx.coroutines.flow.combine(sourceState1.valueFlow, sourceState2.valueFlow, transformation).onEach {
        targetState.value = it
    }.launchIn(this).apply {
        invokeOnCompletion {
            unregisterState(descriptor)
        }
    }
}

/**
 * Register [StateDescriptor] that combines values from [sourceStates] using [transformation].
 *
 * On resulting [Job] cancel the binding is unregistered
 */
public inline fun <reified T, R> StateContainer.combineTo(
    sourceStates: Collection<DeviceState<T>>,
    targetState: MutableDeviceState<R>,
    noinline transformation: suspend (Array<T>) -> R,
): Job {
    val descriptor = StateConnectionDescriptor(sourceStates, setOf(targetState))
    registerState(descriptor)
    return kotlinx.coroutines.flow.combine(sourceStates.map { it.valueFlow }, transformation).onEach {
        targetState.value = it
    }.launchIn(this).apply {
        invokeOnCompletion {
            unregisterState(descriptor)
        }
    }
}