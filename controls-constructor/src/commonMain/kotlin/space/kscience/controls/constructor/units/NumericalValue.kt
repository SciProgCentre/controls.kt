package space.kscience.controls.constructor.units

import kotlin.jvm.JvmInline


/**
 * A value without identity coupled to units of measurements.
 */
@JvmInline
public value class NumericalValue<T: UnitsOfMeasurement>(public val value: Double)