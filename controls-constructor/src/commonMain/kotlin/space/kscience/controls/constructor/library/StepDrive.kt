package space.kscience.controls.constructor.library

import space.kscience.controls.constructor.DeviceState
import space.kscience.controls.constructor.MutableDeviceState
import space.kscience.controls.constructor.combineState
import space.kscience.controls.constructor.property
import space.kscience.controls.constructor.units.*
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.meta.MetaConverter

/**
 * A step drive regulated by [input]
 */
public class StepDrive(
    context: Context,
    public val step: NumericalValue<Degrees>,
    public val zero: NumericalValue<Degrees> = NumericalValue(0.0),
    direction: Direction = Direction.UP,
    input: MutableDeviceState<Int> = MutableDeviceState(0),
    hold: MutableDeviceState<Boolean> = MutableDeviceState(false)
) : Transmission<Int, NumericalValue<Degrees>>(context) {

    override val input: MutableDeviceState<Int> by property(MetaConverter.int, input)

    public val hold: MutableDeviceState<Boolean> by property(MetaConverter.boolean, hold)

    override val output: DeviceState<NumericalValue<Degrees>> = combineState(
        input, hold
    ) { input, hold ->
        //TODO use hold parameter
        zero + input * direction.coef * step
    }
}