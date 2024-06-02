package space.kscience.controls.constructor.models

import space.kscience.controls.constructor.DeviceState
import space.kscience.controls.constructor.ModelConstructor
import space.kscience.controls.constructor.map
import space.kscience.controls.constructor.units.Meters
import space.kscience.controls.constructor.units.Newtons
import space.kscience.controls.constructor.units.NewtonsMeters
import space.kscience.controls.constructor.units.NumericalValue
import space.kscience.dataforge.context.Context

public class ScrewDrive(
    context: Context,
    public val leverage: NumericalValue<Meters>,
) : ModelConstructor(context) {
    public fun transformForce(
        stateOfForce: DeviceState<NumericalValue<NewtonsMeters>>,
    ): DeviceState<NumericalValue<Newtons>> = DeviceState.map(stateOfForce) {
        NumericalValue(it.value * leverage.value)
    }

}