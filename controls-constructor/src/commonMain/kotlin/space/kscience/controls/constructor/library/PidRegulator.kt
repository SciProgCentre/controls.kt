package space.kscience.controls.constructor.library

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.kscience.controls.constructor.DeviceState
import space.kscience.controls.constructor.MutableDeviceState
import space.kscience.controls.constructor.state
import space.kscience.controls.constructor.stateOf
import space.kscience.controls.manager.clock
import space.kscience.dataforge.context.Context
import kotlin.time.Duration
import kotlin.time.DurationUnit


/**
 * Pid regulator parameters
 */
public data class PidParameters(
    val kp: Double,
    val ki: Double,
    val kd: Double,
    val timeStep: Duration,
)

/**
 * A PID regulator
 */
public class PidRegulator(
    context: Context,
    private val position: DeviceState<Double>,
    public var pidParameters: PidParameters, // TODO expose as property
    output: MutableDeviceState<Double> = MutableDeviceState(0.0),
) : Regulator<Double>(context) {

    override val target: MutableDeviceState<Double> = stateOf(0.0)
    override val output: MutableDeviceState<Double> = state(output)

    private var lastPosition: Double = target.value

    private var integral: Double = 0.0

    private val mutex = Mutex()

    private var lastTime = clock.now()

    private val updateJob = launch {
        while (isActive) {
            delay(pidParameters.timeStep)
            mutex.withLock {
                val realTime = clock.now()
                val delta = target.value - position.value
                val dtSeconds = (realTime - lastTime).toDouble(DurationUnit.SECONDS)
                integral += delta * dtSeconds
                val derivative = (position.value - lastPosition) / dtSeconds

                //set last time and value to new values
                lastTime = realTime
                lastPosition = position.value

                output.value = pidParameters.kp * delta + pidParameters.ki * integral + pidParameters.kd * derivative
            }
        }
    }
}

//
//public fun DeviceGroup.pid(
//    name: String,
//    drive: Drive,
//    pidParameters: PidParameters,
//): PidRegulator = install(name.parseAsName(), PidRegulator(drive, pidParameters))