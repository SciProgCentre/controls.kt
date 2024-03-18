package space.kscience.controls.plc4x

import org.apache.plc4x.java.api.messages.PlcReadRequest
import org.apache.plc4x.java.api.messages.PlcReadResponse
import org.apache.plc4x.java.api.messages.PlcWriteRequest
import org.apache.plc4x.java.api.types.PlcValueType
import space.kscience.dataforge.meta.Meta

public interface Plc4xProperty {

    public fun PlcReadRequest.Builder.request(): PlcReadRequest.Builder

    public fun PlcReadResponse.readProperty(): Meta

    public fun PlcWriteRequest.Builder.writeProperty(meta: Meta): PlcWriteRequest.Builder
}

public class DefaultPlc4xProperty(
    private val address: String,
    private val plcValueType: PlcValueType,
    private val name: String = "@default",
) : Plc4xProperty {

    override fun PlcReadRequest.Builder.request(): PlcReadRequest.Builder =
        addTagAddress(name, address)
    override fun PlcReadResponse.readProperty(): Meta =
        asPlcValue.toMeta()

    override fun PlcWriteRequest.Builder.writeProperty(meta: Meta): PlcWriteRequest.Builder =
        addTagAddress(name, address, meta.toPlcValue(plcValueType))
}