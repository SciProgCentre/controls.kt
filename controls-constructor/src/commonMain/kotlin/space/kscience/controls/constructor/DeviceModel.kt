package space.kscience.controls.constructor

import space.kscience.dataforge.context.Context

public abstract class DeviceModel(override val context: Context) : StateContainer {

    private val _stateDescriptors: MutableList<StateDescriptor> = mutableListOf()

    override val stateDescriptors: List<StateDescriptor> get() = _stateDescriptors

    override fun registerState(stateDescriptor: StateDescriptor) {
        _stateDescriptors.add(stateDescriptor)
    }
}