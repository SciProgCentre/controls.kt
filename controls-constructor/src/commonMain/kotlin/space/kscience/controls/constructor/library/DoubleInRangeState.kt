package space.kscience.controls.constructor.library

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import space.kscience.controls.constructor.DeviceState
import space.kscience.controls.constructor.MutableDeviceState
import space.kscience.controls.constructor.map

/**
 *  A state describing a [Double] value in the [range]
 */
public open class DoubleInRangeState(
    private val input: DeviceState<Double>,
    public val range: ClosedFloatingPointRange<Double>,
) : DeviceState<Double> {

    override val valueFlow: Flow<Double> get() = input.valueFlow.map { it.coerceIn(range) }

    override val value: Double get() = input.value.coerceIn(range)

    /**
     * A state showing that the range is on its lower boundary
     */
    public val atStart: DeviceState<Boolean> = input.map { it <= range.start }

    /**
     * A state showing that the range is on its higher boundary
     */
    public val atEnd: DeviceState<Boolean> = input.map { it >= range.endInclusive }

    override fun toString(): String = "DoubleRangeState(value=${value},range=$range)"
}

public class MutableDoubleInRangeState(
    private val mutableInput: MutableDeviceState<Double>,
    range: ClosedFloatingPointRange<Double>
) : DoubleInRangeState(mutableInput, range), MutableDeviceState<Double> {
    override var value: Double
        get() = super.value
        set(value) {
            mutableInput.value = value.coerceIn(range)
        }
}

public fun MutableDoubleInRangeState(
    initialValue: Double,
    range: ClosedFloatingPointRange<Double>
): MutableDoubleInRangeState = MutableDoubleInRangeState(MutableDeviceState(initialValue),range)


public fun DeviceState<Double>.coerceIn(
    range: ClosedFloatingPointRange<Double>
): DoubleInRangeState = DoubleInRangeState(this, range)

public fun MutableDeviceState<Double>.coerceIn(
    range: ClosedFloatingPointRange<Double>
): MutableDoubleInRangeState = MutableDoubleInRangeState(this, range)