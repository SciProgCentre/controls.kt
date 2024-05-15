package space.kscience.controls.constructor

import space.kscience.controls.api.Device

/**
 * A binding that is used to describe device functionality
 */
public sealed interface Binding

/**
 * A binding that exposes device property as read-only state
 */
public class PropertyBinding<T>(
    public val device: Device,
    public val propertyName: String,
    public val state: DeviceState<T>,
) : Binding

/**
 * A binding for independent state like a timer
 */
public class StateBinding<T>(
    public val state: DeviceState<T>
) : Binding

public class ActionBinding(
    public val reads: Collection<DeviceState<*>>,
    public val writes: Collection<DeviceState<*>>
): Binding


public interface BindingsContainer{
    public val bindings: List<Binding>
    public fun registerBinding(binding: Binding)
}