package space.kscience.controls.constructor.library

import space.kscience.controls.api.Device
import space.kscience.controls.constructor.DeviceConstructor
import space.kscience.controls.constructor.DeviceState
import space.kscience.controls.constructor.property
import space.kscience.controls.spec.DevicePropertySpec
import space.kscience.controls.spec.DeviceSpec
import space.kscience.controls.spec.booleanProperty
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.meta.MetaConverter


/**
 * A limit switch device
 */
public interface LimitSwitch : Device {

    public fun isLocked(): Boolean

    public companion object : DeviceSpec<LimitSwitch>() {
        public val locked: DevicePropertySpec<LimitSwitch, Boolean> by booleanProperty { isLocked() }
    }
}

/**
 * Virtual [LimitSwitch]
 */
public class VirtualLimitSwitch(
    context: Context,
    locked: DeviceState<Boolean>,
) : DeviceConstructor(context), LimitSwitch {

    public val locked: DeviceState<Boolean> by property(MetaConverter.boolean, locked)

    override fun isLocked(): Boolean = locked.value
}