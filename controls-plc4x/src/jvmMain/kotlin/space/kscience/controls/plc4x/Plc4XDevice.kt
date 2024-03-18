package space.kscience.controls.plc4x

import kotlinx.coroutines.future.await
import org.apache.plc4x.java.api.PlcConnection
import org.apache.plc4x.java.api.messages.PlcWriteRequest
import space.kscience.controls.api.Device
import space.kscience.dataforge.meta.Meta

public interface Plc4XDevice: Device {
    public val connection: PlcConnection

    public suspend fun read(plc4xProperty: Plc4xProperty): Meta = with(plc4xProperty){
        val request = connection.readRequestBuilder().request().build()
        val response = request.execute().await()
        response.readProperty()
    }

    public suspend fun write(plc4xProperty: Plc4xProperty, value: Meta): Unit = with(plc4xProperty){
        val request: PlcWriteRequest = connection.writeRequestBuilder().writeProperty(value).build()
       request.execute().await()
    }

    public suspend fun subscribe(propertyName: String, plc4xProperty: Plc4xProperty): Unit = with(plc4xProperty){

    }

}

