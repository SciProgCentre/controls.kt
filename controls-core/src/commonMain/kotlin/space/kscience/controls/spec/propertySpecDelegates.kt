package space.kscience.controls.spec

import space.kscience.controls.api.Device
import space.kscience.controls.api.PropertyDescriptor
import space.kscience.controls.api.metaDescriptor
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.ValueType
import space.kscience.dataforge.meta.transformations.MetaConverter
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

//read only delegates

public fun <D : Device> DeviceSpec<D>.booleanProperty(
    descriptorBuilder: PropertyDescriptor.() -> Unit = {},
    name: String? = null,
    read: suspend D.(propertyName: String) -> Boolean?
): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, DevicePropertySpec<D, Boolean>>> = property(
    MetaConverter.boolean,
    {
        metaDescriptor {
            type(ValueType.BOOLEAN)
        }
        descriptorBuilder()
    },
    name,
    read
)

private inline fun numberDescriptor(
    crossinline descriptorBuilder: PropertyDescriptor.() -> Unit = {}
): PropertyDescriptor.() -> Unit = {
    metaDescriptor {
        type(ValueType.NUMBER)
    }
    descriptorBuilder()
}

public fun <D : Device> DeviceSpec<D>.numberProperty(
    descriptorBuilder: PropertyDescriptor.() -> Unit = {},
    name: String? = null,
    read: suspend D.(propertyName: String) -> Number?
): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, DevicePropertySpec<D, Number>>> = property(
    MetaConverter.number,
    numberDescriptor(descriptorBuilder),
    name,
    read
)

public fun <D : Device> DeviceSpec<D>.doubleProperty(
    descriptorBuilder: PropertyDescriptor.() -> Unit = {},
    name: String? = null,
    read: suspend D.(propertyName: String) -> Double?
): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, DevicePropertySpec<D, Double>>> = property(
    MetaConverter.double,
    numberDescriptor(descriptorBuilder),
    name,
    read
)

public fun <D : Device> DeviceSpec<D>.stringProperty(
    descriptorBuilder: PropertyDescriptor.() -> Unit = {},
    name: String? = null,
    read: suspend D.(propertyName: String) -> String?
): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, DevicePropertySpec<D, String>>> = property(
    MetaConverter.string,
    {
        metaDescriptor {
            type(ValueType.STRING)
        }
        descriptorBuilder()
    },
    name,
    read
)

public fun <D : Device> DeviceSpec<D>.metaProperty(
    descriptorBuilder: PropertyDescriptor.() -> Unit = {},
    name: String? = null,
    read: suspend D.(propertyName: String) -> Meta?
): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, DevicePropertySpec<D, Meta>>> = property(
    MetaConverter.meta,
    {
        metaDescriptor {
            type(ValueType.STRING)
        }
        descriptorBuilder()
    },
    name,
    read
)

//read-write delegates

public fun <D : Device> DeviceSpec<D>.booleanProperty(
    descriptorBuilder: PropertyDescriptor.() -> Unit = {},
    name: String? = null,
    read: suspend D.(propertyName: String) -> Boolean?,
    write: suspend D.(propertyName: String, value: Boolean) -> Unit
): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, MutableDevicePropertySpec<D, Boolean>>> =
    mutableProperty(
        MetaConverter.boolean,
        {
            metaDescriptor {
                type(ValueType.BOOLEAN)
            }
            descriptorBuilder()
        },
        name,
        read,
        write
    )


public fun <D : Device> DeviceSpec<D>.numberProperty(
    descriptorBuilder: PropertyDescriptor.() -> Unit = {},
    name: String? = null,
    read: suspend D.(propertyName: String) -> Number,
    write: suspend D.(propertyName: String, value: Number) -> Unit
): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, MutableDevicePropertySpec<D, Number>>> =
    mutableProperty(MetaConverter.number, numberDescriptor(descriptorBuilder), name, read, write)

public fun <D : Device> DeviceSpec<D>.doubleProperty(
    descriptorBuilder: PropertyDescriptor.() -> Unit = {},
    name: String? = null,
    read: suspend D.(propertyName: String) -> Double,
    write: suspend D.(propertyName: String, value: Double) -> Unit
): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, MutableDevicePropertySpec<D, Double>>> =
    mutableProperty(MetaConverter.double, numberDescriptor(descriptorBuilder), name, read, write)

public fun <D : Device> DeviceSpec<D>.stringProperty(
    descriptorBuilder: PropertyDescriptor.() -> Unit = {},
    name: String? = null,
    read: suspend D.(propertyName: String) -> String,
    write: suspend D.(propertyName: String, value: String) -> Unit
): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, MutableDevicePropertySpec<D, String>>> =
    mutableProperty(MetaConverter.string, descriptorBuilder, name, read, write)

public fun <D : Device> DeviceSpec<D>.metaProperty(
    descriptorBuilder: PropertyDescriptor.() -> Unit = {},
    name: String? = null,
    read: suspend D.(propertyName: String) -> Meta,
    write: suspend D.(propertyName: String, value: Meta) -> Unit
): PropertyDelegateProvider<DeviceSpec<D>, ReadOnlyProperty<DeviceSpec<D>, MutableDevicePropertySpec<D, Meta>>> =
    mutableProperty(MetaConverter.meta, descriptorBuilder, name, read, write)