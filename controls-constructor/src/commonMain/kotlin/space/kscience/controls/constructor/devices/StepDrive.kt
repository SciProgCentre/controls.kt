package space.kscience.controls.constructor.devices

import kotlinx.coroutines.launch
import space.kscience.controls.constructor.*
import space.kscience.controls.constructor.units.Degrees
import space.kscience.controls.constructor.units.NumericalValue
import space.kscience.controls.constructor.units.plus
import space.kscience.controls.constructor.units.times
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.meta.MetaConverter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.DurationUnit

/**
 * A step drive
 *
 * @param speed ticks per second
 * @param target target ticks state
 * @param writeTicks a hardware callback
 */
public class StepDrive(
    context: Context,
    speed: MutableDeviceState<Double>,
    target: MutableDeviceState<Int> = MutableDeviceState(0),
    private val writeTicks: suspend (ticks: Int, speed: Double) -> Unit,
) : DeviceConstructor(context) {

    public val target: MutableDeviceState<Int> by property(MetaConverter.int, target)

    public val speed: MutableDeviceState<Double> by property(MetaConverter.double, speed)

    private val positionState = stateOf(target.value)

    public val position: DeviceState<Int> by property(MetaConverter.int, positionState)

    private val ticker = onTimer { prev, next ->
        val tickSpeed = speed.value
        val timeDelta = (next - prev).toDouble(DurationUnit.SECONDS)
        val ticksDelta: Int = target.value - position.value
        val steps: Int = when {
            ticksDelta > 0 -> min(ticksDelta, (timeDelta * tickSpeed).roundToInt())
            ticksDelta < 0 -> max(ticksDelta, (timeDelta * tickSpeed).roundToInt())
            else -> return@onTimer
        }
        launch {
            writeTicks(steps, tickSpeed)
            positionState.value += steps
        }
    }
}

/**
 * Compute a state using given tick-to-angle transformation
 */
public fun StepDrive.angle(
    zero: NumericalValue<Degrees>,
    step: NumericalValue<Degrees>,
): DeviceState<NumericalValue<Degrees>> = position.map { zero + it * step }

