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
public interface PidParameters {
    public val kp: Double
    public val ki: Double
    public val kd: Double
    public val timeStep: Duration
}

private data class PidParametersImpl(
    override val kp: Double,
    override val ki: Double,
    override val kd: Double,
    override val timeStep: Duration,
) : PidParameters

public fun PidParameters(kp: Double, ki: Double, kd: Double, timeStep: Duration = 1.milliseconds): PidParameters =
    PidParametersImpl(kp, ki, kd, timeStep)

/**
 * A drive with PID regulator
 */
public class PidRegulator(
    public val drive: Drive,
    public val pidParameters: PidParameters,
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
                    val delta = target - position
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

    override val position: Double get() = drive.position
}

public fun DeviceGroup.pid(
    name: String,
    drive: Drive,
    pidParameters: PidParameters,
): PidRegulator = install(name.parseAsName(), PidRegulator(drive, pidParameters))