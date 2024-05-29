package space.kscience.controls.constructor.library

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import space.kscience.controls.constructor.DeviceGroup
import space.kscience.controls.manager.clock
import space.kscience.controls.spec.DeviceBySpec
import space.kscience.controls.spec.write
import space.kscience.dataforge.names.parseAsName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit


/**
 * Pid regulator parameters
 */
public data class PidParameters(
    val kp: Double,
    val ki: Double,
    val kd: Double,
    val timeStep: Duration  = 1.milliseconds,
)
/**
 * A drive with PID regulator
 */
public class PidRegulator(
    public val drive: Drive,
    public var pidParameters: PidParameters, // TODO expose as property
) : DeviceBySpec<Regulator>(Regulator, drive.context), Regulator {

    private val clock = drive.context.clock

    override var target: Double = drive.position

    private var lastTime: Instant = clock.now()
    private var lastPosition: Double = target

    private var integral: Double = 0.0


    private var updateJob: Job? = null
    private val mutex = Mutex()


    override suspend fun onStart() {
        drive.start()
        updateJob = launch {
            while (isActive) {
                delay(pidParameters.timeStep)
                mutex.withLock {
                    val realTime = clock.now()
                    val delta = target - getPosition()
                    val dtSeconds = (realTime - lastTime).toDouble(DurationUnit.SECONDS)
                    integral += delta * dtSeconds
                    val derivative = (drive.position - lastPosition) / dtSeconds

                    //set last time and value to new values
                    lastTime = realTime
                    lastPosition = drive.position

                    drive.write(Drive.force,pidParameters.kp * delta + pidParameters.ki * integral + pidParameters.kd * derivative)
                    //drive.force = pidParameters.kp * delta + pidParameters.ki * integral + pidParameters.kd * derivative
                    propertyChanged(Regulator.position, drive.position)
                }
            }
        }
    }

    override suspend fun onStop() {
        updateJob?.cancel()
        drive.stop()
    }

    override suspend fun getPosition(): Double = drive.position
}

public fun DeviceGroup.pid(
    name: String,
    drive: Drive,
    pidParameters: PidParameters,
): PidRegulator = install(name.parseAsName(), PidRegulator(drive, pidParameters))