package space.kscience.controls.constructor.models

import space.kscience.controls.constructor.DeviceState
import space.kscience.controls.constructor.ModelConstructor
import space.kscience.controls.constructor.map
import space.kscience.controls.constructor.units.*
import space.kscience.dataforge.context.Context
import kotlin.math.PI

public class ScrewDrive(
    context: Context,
    public val leverage: NumericalValue<Meters>,
) : ModelConstructor(context) {

    public fun torqueToForce(
        stateOfMomentum: DeviceState<NumericalValue<NewtonsMeters>>,
    ): DeviceState<NumericalValue<Newtons>> = DeviceState.map(stateOfMomentum) { momentum ->
        NumericalValue(momentum.value / leverage.value )
    }

    public fun degreesToMeters(
        stateOfAngle: DeviceState<NumericalValue<Degrees>>,
        offset: NumericalValue<Meters> = NumericalValue(0),
    ): DeviceState<NumericalValue<Meters>> = DeviceState.map(stateOfAngle) { degrees ->
        offset + NumericalValue(degrees.value * 2 * PI / 360 *leverage.value )
    }

}