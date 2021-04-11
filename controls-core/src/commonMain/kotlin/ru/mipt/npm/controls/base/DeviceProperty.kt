package space.kscience.dataforge.control.base

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import space.kscience.dataforge.control.api.PropertyDescriptor
import space.kscience.dataforge.meta.MetaItem
import kotlin.time.Duration

/**
 * Read-only device property
 */
public interface ReadOnlyDeviceProperty {
    /**
     * Property name, should be unique in device
     */
    public val name: String

    /**
     * Property descriptor
     */
    public val descriptor: PropertyDescriptor

    public val scope: CoroutineScope

    /**
     * Erase logical value and force re-read from device on next [read]
     */
    public suspend fun invalidate()

    /**
     * Directly update property logical value and notify listener without writing it to device
     */
    public fun updateLogical(item: MetaItem)

    /**
     *  Get cached value and return null if value is invalid or not initialized
     */
    public val value: MetaItem?

    /**
     * Read value either from cache if cache is valid or directly from physical device.
     * If [force], reread from physical state even if the logical state is set.
     */
    public suspend fun read(force: Boolean = false): MetaItem

    /**
     * The [Flow] representing future logical states of the property.
     * Produces null when the state is invalidated
     */
    public fun flow(): Flow<MetaItem?>
}


/**
 * Launch recurring force re-read job on a property scope with given [duration] between reads.
 */
public fun ReadOnlyDeviceProperty.readEvery(duration: Duration): Job = scope.launch {
    while (isActive) {
        read(true)
        delay(duration)
    }
}

/**
 * A writeable device property with non-suspended write
 */
public interface DeviceProperty : ReadOnlyDeviceProperty {
    override var value: MetaItem?

    /**
     * Write value to physical device. Invalidates logical value, but does not update it automatically
     */
    public suspend fun write(item: MetaItem)
}