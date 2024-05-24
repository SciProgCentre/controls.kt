package space.kscience.controls.constructor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.newCoroutineContext
import space.kscience.dataforge.context.Context
import kotlin.coroutines.CoroutineContext

public abstract class DeviceModel(
    final override val context: Context,
    vararg dependencies: DeviceState<*>,
) : StateContainer, CoroutineScope {

    override val coroutineContext: CoroutineContext = context.newCoroutineContext(SupervisorJob())


    private val _stateDescriptors: MutableSet<StateDescriptor> = mutableSetOf<StateDescriptor>().apply {
        dependencies.forEach {
            add(StateNodeDescriptor(it))
        }
    }

    override val stateDescriptors: Set<StateDescriptor> get() = _stateDescriptors

    override fun registerState(stateDescriptor: StateDescriptor) {
        _stateDescriptors.add(stateDescriptor)
    }

    override fun unregisterState(stateDescriptor: StateDescriptor) {
        _stateDescriptors.remove(stateDescriptor)
    }
}