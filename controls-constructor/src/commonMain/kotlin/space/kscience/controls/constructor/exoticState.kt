package space.kscience.controls.constructor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


/**
 *  A state describing a [Double] value in the [range]
 */
public class DoubleInRangeState(
    initialValue: Double,
    public val range: ClosedFloatingPointRange<Double>,
) : MutableDeviceState<Double> {

    init {
        require(initialValue in range) { "Initial value should be in range" }
    }


    private val _valueFlow = MutableStateFlow(initialValue)

    override var value: Double
        get() = _valueFlow.value
        set(newValue) {
            _valueFlow.value = newValue.coerceIn(range)
        }

    override val valueFlow: StateFlow<Double> get() = _valueFlow

    /**
     * A state showing that the range is on its lower boundary
     */
    public val atStart: DeviceState<Boolean> = DeviceState.map(this) {
        it <= range.start
    }

    /**
     * A state showing that the range is on its higher boundary
     */
    public val atEnd: DeviceState<Boolean> = DeviceState.map(this) {
        it >= range.endInclusive
    }

    override fun toString(): String = "DoubleRangeState(range=$range)"


}

/**
 * Create and register a [DoubleInRangeState]
 */
public fun StateContainer.doubleInRangeState(
    initialValue: Double,
    range: ClosedFloatingPointRange<Double>,
): DoubleInRangeState = DoubleInRangeState(initialValue, range).also {
    registerState(StateNodeDescriptor(it))
}