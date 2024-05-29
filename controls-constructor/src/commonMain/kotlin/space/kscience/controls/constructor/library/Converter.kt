package space.kscience.controls.constructor.library

import kotlinx.coroutines.flow.FlowCollector
import space.kscience.controls.constructor.DeviceConstructor
import space.kscience.controls.constructor.DeviceState
import space.kscience.controls.constructor.DeviceStateWithDependencies
import space.kscience.controls.constructor.flowState
import space.kscience.dataforge.context.Context

/**
 * A device that converts one type of physical quantity to another type
 */
public class Converter<T, R>(
    context: Context,
    input: DeviceState<T>,
    initialValue: R,
    transform: suspend FlowCollector<R>.(T) -> Unit,
) : DeviceConstructor(context) {
    public val output: DeviceStateWithDependencies<R> = flowState(input, initialValue, transform)
}