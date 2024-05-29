package space.kscience.controls.constructor.units


public interface UnitsOfMeasurement

/**/

public interface UnitsOfLength : UnitsOfMeasurement

public data object Meters: UnitsOfLength

/**/

public interface UnitsOfTime: UnitsOfMeasurement

public data object Seconds: UnitsOfTime

/**/

public interface UnitsOfVelocity: UnitsOfMeasurement

public data object MetersPerSecond: UnitsOfVelocity

/**/

public interface UnitsAngularOfVelocity: UnitsOfMeasurement

public data object RadiansPerSecond: UnitsAngularOfVelocity

public data object DegreesPerSecond: UnitsAngularOfVelocity