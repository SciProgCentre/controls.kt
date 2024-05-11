package space.kscience.controls.constructor.library

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import space.kscience.controls.api.Device
import space.kscience.controls.constructor.DeviceState
import space.kscience.controls.spec.DeviceBySpec
import space.kscience.controls.spec.DevicePropertySpec
import space.kscience.controls.spec.DeviceSpec
import space.kscience.controls.spec.booleanProperty
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Factory


/**
 * A limit switch device
 */
public interface LimitSwitch : Device {

    public val locked: Boolean

    public companion object : DeviceSpec<LimitSwitch>() {
        public val locked: DevicePropertySpec<LimitSwitch, Boolean> by booleanProperty { locked }
        public operator fun invoke(lockedState: DeviceState<Boolean>): Factory<LimitSwitch> = Factory { context, _ ->
            VirtualLimitSwitch(context, lockedState)
        }
    }
}

/**
 * Virtual [LimitSwitch]
 */
public class VirtualLimitSwitch(
    context: Context,
    public val lockedState: DeviceState<Boolean>,
) : DeviceBySpec<LimitSwitch>(LimitSwitch, context), LimitSwitch {

    override suspend fun onStart() {
        lockedState.valueFlow.onEach {
            propertyChanged(LimitSwitch.locked, it)
        }.launchIn(this)
    }

    override val locked: Boolean get() = lockedState.value
}