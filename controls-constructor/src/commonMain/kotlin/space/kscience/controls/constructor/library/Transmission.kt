package space.kscience.controls.constructor.library

import space.kscience.controls.constructor.DeviceConstructor
import space.kscience.controls.constructor.DeviceState
import space.kscience.controls.constructor.MutableDeviceState
import space.kscience.controls.constructor.flowState
import space.kscience.dataforge.context.Context


/**
 * A model for a device that converts one type of physical quantity to another type
 */
public abstract class Transmission<T, R>(context: Context) : DeviceConstructor(context) {
    public abstract val input: MutableDeviceState<T>
    public abstract val output: DeviceState<R>

    public companion object {
        /**
         * Create a device that is a hard connection between two physical quantities
         */
        public suspend fun <T, R> direct(
            context: Context,
            input: MutableDeviceState<T>,
            transform: suspend (T) -> R
        ): Transmission<T, R> {
            val initialValue = transform(input.value)
            return object : Transmission<T, R>(context) {
                override val input: MutableDeviceState<T> = input
                override val output: DeviceState<R> = flowState(input, initialValue) { emit(transform(it)) }
            }
        }
    }
}