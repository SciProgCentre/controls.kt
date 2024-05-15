package space.kscience.controls.demo.constructor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import space.kscience.controls.constructor.DeviceConstructor
import space.kscience.controls.constructor.DoubleInRangeState
import space.kscience.controls.constructor.device
import space.kscience.controls.constructor.deviceProperty
import space.kscience.controls.constructor.library.*
import space.kscience.controls.manager.ClockManager
import space.kscience.controls.manager.DeviceManager
import space.kscience.controls.manager.clock
import space.kscience.controls.manager.install
import space.kscience.controls.spec.doRecurring
import space.kscience.controls.spec.name
import space.kscience.controls.vision.plot
import space.kscience.controls.vision.plotDeviceProperty
import space.kscience.controls.vision.plotNumberState
import space.kscience.controls.vision.showDashboard
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.meta.Meta
import space.kscience.plotly.models.ScatterMode
import space.kscience.visionforge.plotly.PlotlyPlugin
import kotlin.math.PI
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit


class LinearDrive(
    drive: Drive,
    start: LimitSwitch,
    end: LimitSwitch,
    pidParameters: PidParameters,
    meta: Meta = Meta.EMPTY,
) : DeviceConstructor(drive.context, meta) {

    val drive: Drive by device(drive)
    val pid by device(PidRegulator(drive, pidParameters))

    val start by device(start)
    val end by device(end)

    val position by deviceProperty(drive, Drive.position, Double.NaN)

    val target by deviceProperty(pid, Regulator.target, 0.0)
}

/**
 * A shortcut to create a virtual [LimitSwitch] from [DoubleInRangeState]
 */
fun LinearDrive(
    context: Context,
    positionState: DoubleInRangeState,
    mass: Double,
    pidParameters: PidParameters,
    meta: Meta = Meta.EMPTY,
): LinearDrive = LinearDrive(
    drive = VirtualDrive(context, mass, positionState),
    start = VirtualLimitSwitch(context, positionState.atStart),
    end = VirtualLimitSwitch(context, positionState.atEnd),
    pidParameters = pidParameters,
    meta = meta
)


fun main() = application {
    val context = Context {
        plugin(DeviceManager)
        plugin(PlotlyPlugin)
        plugin(ClockManager)
    }

    class MutablePidParameters(
        kp: Double,
        ki: Double,
        kd: Double,
        timeStep: Duration,
    ) : PidParameters {
        override var kp by mutableStateOf(kp)
        override var ki by mutableStateOf(ki)
        override var kd by mutableStateOf(kd)
        override var timeStep by mutableStateOf(timeStep)
    }

    val pidParameters = remember {
        MutablePidParameters(
            kp = 2.5,
            ki = 0.0,
            kd = -0.1,
            timeStep = 0.005.seconds
        )
    }

    val state = DoubleInRangeState(0.0, -6.0..6.0)

    val linearDrive = context.install(
        "linearDrive",
        LinearDrive(context, state, 0.05, pidParameters)
    )

    val clockStart = context.clock.now()
    linearDrive.doRecurring(10.milliseconds) {
        val timeFromStart = clock.now() - clockStart
        val t = timeFromStart.toDouble(DurationUnit.SECONDS)
        val freq = 0.1
        target.value = 5 * sin(2.0 * PI * freq * t) +
                sin(2 * PI * 21 * freq * t + 0.02 * (timeFromStart / pidParameters.timeStep))
    }


    val maxAge = 10.seconds

    context.showDashboard {
        plot {
            plotNumberState(context, state, maxAge = maxAge, sampling = 50.milliseconds) {
                name = "real position"
            }
            plotDeviceProperty(linearDrive.pid, Regulator.position.name, maxAge = maxAge, sampling = 50.milliseconds) {
                name = "read position"
            }

            plotDeviceProperty(linearDrive.pid, Regulator.target.name, maxAge = maxAge, sampling = 50.milliseconds) {
                name = "target"
            }
        }

        plot {
            plotDeviceProperty(
                linearDrive.start,
                LimitSwitch.locked.name,
                maxAge = maxAge,
                sampling = 50.milliseconds
            ) {
                name = "start measured"
                mode = ScatterMode.markers
            }
            plotDeviceProperty(linearDrive.end, LimitSwitch.locked.name, maxAge = maxAge, sampling = 50.milliseconds) {
                name = "end measured"
                mode = ScatterMode.markers
            }
        }

    }

    Window(title = "Pid regulator simulator", onCloseRequest = ::exitApplication) {
        MaterialTheme {
            Column {
                Row {
                    Text("kp:", Modifier.align(Alignment.CenterVertically).width(50.dp).padding(5.dp))
                    TextField(
                        String.format("%.2f", pidParameters.kp),
                        { pidParameters.kp = it.toDouble() },
                        Modifier.width(100.dp),
                        enabled = false
                    )
                    Slider(
                        pidParameters.kp.toFloat(),
                        { pidParameters.kp = it.toDouble() },
                        valueRange = 0f..20f,
                        steps = 100
                    )
                }
                Row {
                    Text("ki:", Modifier.align(Alignment.CenterVertically).width(50.dp).padding(5.dp))
                    TextField(
                        String.format("%.2f", pidParameters.ki),
                        { pidParameters.ki = it.toDouble() },
                        Modifier.width(100.dp),
                        enabled = false
                    )

                    Slider(
                        pidParameters.ki.toFloat(),
                        { pidParameters.ki = it.toDouble() },
                        valueRange = -10f..10f,
                        steps = 100
                    )
                }
                Row {
                    Text("kd:", Modifier.align(Alignment.CenterVertically).width(50.dp).padding(5.dp))
                    TextField(
                        String.format("%.2f", pidParameters.kd),
                        { pidParameters.kd = it.toDouble() },
                        Modifier.width(100.dp),
                        enabled = false
                    )

                    Slider(
                        pidParameters.kd.toFloat(),
                        { pidParameters.kd = it.toDouble() },
                        valueRange = -10f..10f,
                        steps = 100
                    )
                }

                Row {
                    Text("dt:", Modifier.align(Alignment.CenterVertically).width(50.dp).padding(5.dp))
                    TextField(
                        pidParameters.timeStep.toString(DurationUnit.MILLISECONDS),
                        { pidParameters.timeStep = it.toDouble().milliseconds },
                        Modifier.width(100.dp),
                        enabled = false
                    )

                    Slider(
                        pidParameters.timeStep.toDouble(DurationUnit.MILLISECONDS).toFloat(),
                        { pidParameters.timeStep = it.toDouble().milliseconds },
                        valueRange = 0f..100f,
                        steps = 100
                    )
                }
                Row {
                    Button({
                        pidParameters.run {
                            kp = 2.5
                            ki = 0.0
                            kd = -0.1
                            timeStep = 0.005.seconds
                        }
                    }) {
                        Text("Reset")
                    }
                }
            }
        }
    }
}