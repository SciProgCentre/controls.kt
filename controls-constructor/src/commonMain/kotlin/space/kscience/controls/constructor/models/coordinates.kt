package space.kscience.controls.constructor.models

import space.kscience.controls.constructor.units.NumericalValue
import space.kscience.controls.constructor.units.UnitsOfMeasurement

public data class XY<U: UnitsOfMeasurement>(val x: NumericalValue<U>, val y: NumericalValue<U>)

public data class XYZ<U: UnitsOfMeasurement>(val x: NumericalValue<U>, val y: NumericalValue<U>, val z: NumericalValue<U>)