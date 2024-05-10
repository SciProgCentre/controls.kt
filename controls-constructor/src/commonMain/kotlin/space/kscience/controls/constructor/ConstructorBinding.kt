package space.kscience.controls.constructor

import space.kscience.controls.api.Device

public sealed interface ConstructorBinding

/**
 * A binding that exposes device property as read-only state
 */
public class PropertyBinding<T>(
    public val device: Device,
    public val propertyName: String,
    public val state: DeviceState<T>,
) : ConstructorBinding

/**
 * A binding for independent state like a timer
 */
public class StateBinding<T>(
    public val state: DeviceState<T>
) : ConstructorBinding

public class ActionBinding(
    public val reads: Collection<DeviceState<*>>,
    public val writes: Collection<DeviceState<*>>
): ConstructorBinding