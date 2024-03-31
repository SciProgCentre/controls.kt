package space.kscience.controls.pi

import com.pi4j.io.serial.Baud
import com.pi4j.io.serial.Serial
import com.pi4j.io.serial.SerialConfigBuilder
import com.pi4j.ktx.io.serial
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.kscience.controls.ports.SynchronousPort
import space.kscience.controls.ports.copyToArray
import space.kscience.dataforge.context.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.enum
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import java.nio.ByteBuffer

public class SynchronousPiPort(
    override val context: Context,
    public val meta: Meta,
    private val serial: Serial,
    private val mutex: Mutex = Mutex(),
) : SynchronousPort {

    private val pi = context.request(PiPlugin)
    override fun open() {
        serial.open()
    }

    override val isOpen: Boolean get() = serial.isOpen

    override suspend fun <R> respond(
        request: ByteArray,
        transform: suspend Flow<ByteArray>.() -> R,
    ): R = mutex.withLock {
        serial.drain()
        serial.write(request)
        flow<ByteArray> {
            val buffer = ByteBuffer.allocate(1024)
            while (isOpen) {
                try {
                    val num = serial.read(buffer)
                    if (num > 0) {
                        emit(buffer.copyToArray(num))
                    }
                    if (num < 0) break
                } catch (ex: Exception) {
                    logger.error(ex) { "Channel read error" }
                    delay(1000)
                }
            }
        }.transform()
    }

    override fun close() {
        serial.close()
    }

    public companion object : Factory<SynchronousPort> {


        public fun build(
            context: Context,
            device: String,
            block: SerialConfigBuilder.() -> Unit,
        ): SynchronousPiPort {
            val meta = Meta {
                "name" put "pi://$device"
                "type" put "serial"
            }
            val pi = context.request(PiPlugin)

            val serial = pi.piContext.serial(device, block)
            return SynchronousPiPort(context, meta, serial)
        }

        public fun open(
            context: Context,
            device: String,
            block: SerialConfigBuilder.() -> Unit,
        ): SynchronousPiPort = build(context, device, block).apply { open() }

        override fun build(context: Context, meta: Meta): SynchronousPiPort {
            val device: String = meta["device"].string ?: error("Device name not defined")
            val baudRate: Baud = meta["baudRate"].enum<Baud>() ?: Baud._9600
            val pi = context.request(PiPlugin)
            val serial = pi.piContext.serial(device) {
                baud8N1(baudRate)
            }
            return SynchronousPiPort(context, meta, serial)
        }

    }
}

