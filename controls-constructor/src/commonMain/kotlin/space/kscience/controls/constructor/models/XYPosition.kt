package space.kscience.controls.constructor.models

import space.kscience.controls.constructor.ModelConstructor
import space.kscience.controls.constructor.MutableDeviceState
import space.kscience.controls.constructor.stateOf
import space.kscience.controls.constructor.units.Meters
import space.kscience.controls.constructor.units.NumericalValue
import space.kscience.dataforge.context.Context

public class XYPosition(
    context: Context,
    initialX: NumericalValue<Meters> = NumericalValue(0.0),
    initialY: NumericalValue<Meters> = NumericalValue(0.0),
) : ModelConstructor(context) {
    public val x: MutableDeviceState<NumericalValue<Meters>> = stateOf(initialX)
    public val y: MutableDeviceState<NumericalValue<Meters>> = stateOf(initialY)
}