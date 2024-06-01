package space.kscience.controls.constructor.units

import kotlin.jvm.JvmInline


/**
 * A value without identity coupled to units of measurements.
 */
@JvmInline
public value class NumericalValue<U : UnitsOfMeasurement>(public val value: Double)

public operator fun <U: UnitsOfMeasurement> NumericalValue<U>.compareTo(other: NumericalValue<U>): Int =
    value.compareTo(other.value)

public operator fun <U : UnitsOfMeasurement> NumericalValue<U>.plus(
    other: NumericalValue<U>
): NumericalValue<U> = NumericalValue(this.value + other.value)

public operator fun <U : UnitsOfMeasurement> NumericalValue<U>.minus(
    other: NumericalValue<U>
): NumericalValue<U> = NumericalValue(this.value - other.value)

public operator fun <U : UnitsOfMeasurement> NumericalValue<U>.times(
    c: Number
): NumericalValue<U> = NumericalValue(this.value * c.toDouble())

public operator fun <U : UnitsOfMeasurement> Number.times(
    numericalValue: NumericalValue<U>
): NumericalValue<U> = NumericalValue(numericalValue.value * toDouble())

public operator fun <U : UnitsOfMeasurement> NumericalValue<U>.times(
    c: Double
): NumericalValue<U> = NumericalValue(this.value * c)

public operator fun <U : UnitsOfMeasurement> NumericalValue<U>.div(
    c: Number
): NumericalValue<U> = NumericalValue(this.value / c.toDouble())