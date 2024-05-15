package space.kscience.controls.constructor

public abstract class DeviceModel : BindingsContainer {

    private val _bindings: MutableList<Binding> = mutableListOf()

    override val bindings: List<Binding> get() = _bindings

    override fun registerBinding(binding: Binding) {
        _bindings.add(binding)
    }
}