package space.kscience.controls.constructor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import space.kscience.dataforge.meta.MetaConverter


private class StateFlowAsState<T>(
    override val converter: MetaConverter<T>,
    val flow: MutableStateFlow<T>,
) : MutableDeviceState<T> {
    override var value: T by flow::value
    override val valueFlow: Flow<T> get() = flow

    override fun toString(): String = "FlowAsState(converter=$converter)"
}

/**
 * Create a read-only [DeviceState] that wraps [MutableStateFlow].
 * No data copy is performed.
 */
public fun <T> MutableStateFlow<T>.asDeviceState(converter: MetaConverter<T>): DeviceState<T> =
    StateFlowAsState(converter, this)