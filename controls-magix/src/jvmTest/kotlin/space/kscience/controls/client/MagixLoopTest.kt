package space.kscience.controls.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import space.kscience.controls.client.RemoteDeviceConnect.TestDevice
import space.kscience.controls.manager.DeviceManager
import space.kscience.controls.manager.install
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.request
import space.kscience.magix.api.MagixEndpoint
import space.kscience.magix.rsocket.rSocketWithWebSockets
import space.kscience.magix.server.startMagixServer
import kotlin.test.Test
import kotlin.test.assertEquals

class MagixLoopTest {

    @Test
    fun deviceHub() = runTest {
        val context = Context {
            plugin(DeviceManager)
        }

        val server = context.startMagixServer()

        val deviceManager = context.request(DeviceManager)

        val deviceEndpoint = MagixEndpoint.rSocketWithWebSockets("localhost")

//        deviceEndpoint.subscribe().onEach {
//            println(it)
//        }.launchIn(this)

        deviceManager.launchMagixService(deviceEndpoint, "device")

        launch {
            delay(50)
            repeat(10) {
                deviceManager.install("test[$it]", TestDevice)
            }
        }

        val clientEndpoint = MagixEndpoint.rSocketWithWebSockets("localhost")

        val remoteHub = clientEndpoint.remoteDeviceHub(context, "client", "device")

        assertEquals(0, remoteHub.devices.size)

        delay(60)
        //switch context to use actual delay
        withContext(Dispatchers.Default) {
            clientEndpoint.requestDeviceUpdate("client", "device")
            delay(60)
            assertEquals(10, remoteHub.devices.size)
        }
    }
}