package space.kscience.controls.constructor.models

import space.kscience.controls.constructor.*
import space.kscience.controls.constructor.units.*
import space.kscience.dataforge.context.Context
import kotlin.math.pow
import kotlin.time.DurationUnit


/**
 * 3D material point
 */
public class MaterialPoint(
    context: Context,
    force: DeviceState<XYZ<Newtons>>,
    mass: NumericalValue<Kilograms>,
    public val position: MutableDeviceState<XYZ<Meters>>,
    public val velocity: MutableDeviceState<XYZ<MetersPerSecond>>,
) : ModelConstructor(context) {

    init {
        registerState(position)
        registerState(velocity)
    }

    private var currentForce = force.value

    private val movement = onTimer(
        reads = setOf(velocity, position),
        writes = setOf(velocity, position)
    ) { prev, next ->
        val dtSeconds = (next - prev).toDouble(DurationUnit.SECONDS)

        // compute new value based on velocity and acceleration from the previous step
        position.value += (velocity.value * dtSeconds).cast(Meters) +
                (currentForce / mass.value * dtSeconds.pow(2) / 2).cast(Meters)

        // compute new velocity based on acceleration on the previous step
        velocity.value += (currentForce / mass.value * dtSeconds).cast(MetersPerSecond)
        currentForce = force.value
    }
}