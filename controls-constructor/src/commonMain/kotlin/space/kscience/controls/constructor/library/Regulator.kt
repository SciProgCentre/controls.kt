package space.kscience.controls.constructor.library

import space.kscience.controls.constructor.DeviceConstructor
import space.kscience.controls.constructor.DeviceState
import space.kscience.controls.constructor.MutableDeviceState
import space.kscience.dataforge.context.Context


/**
 * A regulator with target value and current position
 */
public abstract class Regulator<T>(context: Context) : DeviceConstructor(context) {

    public abstract val target: MutableDeviceState<T>

    public abstract val output: DeviceState<T>
}