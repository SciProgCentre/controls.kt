package space.kscience.controls.demo.constructor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.legend.FlowLegend
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.toString
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberDoubleLinearAxisModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Instant
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import space.kscience.controls.compose.PlotDeviceProperty
import space.kscience.controls.compose.PlotNumberState
import space.kscience.controls.compose.TimeAxisModel
import space.kscience.controls.constructor.*
import space.kscience.controls.constructor.library.*
import space.kscience.controls.manager.ClockManager
import space.kscience.controls.manager.DeviceManager
import space.kscience.controls.manager.clock
import space.kscience.controls.manager.install
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.meta.Meta
import java.awt.Dimension
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

    val drive by device(drive)
    val pid by device(
        PidRegulator(
            context = context,
            position = drive.propertyAsState(Drive.position, 0.0),
            pidParameters = pidParameters
        )
    )

    private val binding = bind(pid.output, drive.stateOfForce())

    val start by device(start)
    val end by device(end)
}

/**
 * A shortcut to create a virtual [LimitSwitch] from [DoubleInRangeState]
 */
fun LinearDrive(
    context: Context,
    positionState: MutableDoubleInRangeState,
    mass: Double,
    pidParameters: PidParameters,
    meta: Meta = Meta.EMPTY,
): LinearDrive = LinearDrive(
    drive = VirtualDrive(context, mass, positionState),
    start = LimitSwitch(context, positionState.atStart),
    end = LimitSwitch(context, positionState.atEnd),
    pidParameters = pidParameters,
    meta = meta
)

class Modulator(
    context: Context,
    target: MutableDeviceState<Double>,
    var freq: Double = 0.1,
    var timeStep: Duration = 5.milliseconds,
) : DeviceConstructor(context) {
    private val clockStart = clock.now()

    val timer = timer(10.milliseconds)

    private val modulation = timer.onNext {
        val timeFromStart = clock.now() - clockStart
        val t = timeFromStart.toDouble(DurationUnit.SECONDS)
        target.value = 5 * sin(2.0 * PI * freq * t) +
                sin(2 * PI * 21 * freq * t + 0.02 * (timeFromStart / timeStep))
    }
}


private val maxAge = 10.seconds

@OptIn(ExperimentalSplitPaneApi::class, ExperimentalKoalaPlotApi::class)
fun main() = application {
    val context = remember {
        Context {
            plugin(DeviceManager)
            plugin(ClockManager)
        }
    }

    val clock = remember { context.clock }


    var pidParameters by remember {
        mutableStateOf(PidParameters(kp = 2.5, ki = 0.0, kd = -0.1, timeStep = 0.005.seconds))
    }

    val state = remember { MutableDoubleInRangeState(0.0, -6.0..6.0) }

    val linearDrive = remember {
        context.install(
            "linearDrive",
            LinearDrive(context, state, 0.05, pidParameters)
        )
    }

    val modulator = remember {
        context.install(
            "modulator",
            Modulator(context, linearDrive.pid.target)
        )
    }

    //bind pid parameters
    LaunchedEffect(Unit) {
        snapshotFlow {
            pidParameters
        }.onEach {
            linearDrive.pid.pidParameters = pidParameters
        }.collect()
    }

    Window(title = "Pid regulator simulator", onCloseRequest = ::exitApplication) {
        window.minimumSize = Dimension(800, 400)
        MaterialTheme {
            HorizontalSplitPane {
                first(400.dp) {
                    Column(modifier = Modifier.background(color = Color.LightGray).fillMaxHeight()) {
                        Row {
                            Text("kp:", Modifier.align(Alignment.CenterVertically).width(50.dp).padding(5.dp))
                            TextField(
                                String.format("%.2f", pidParameters.kp),
                                { pidParameters = pidParameters.copy(kp = it.toDouble()) },
                                Modifier.width(100.dp),
                                enabled = false
                            )
                            Slider(
                                pidParameters.kp.toFloat(),
                                { pidParameters = pidParameters.copy(kp = it.toDouble()) },
                                valueRange = 0f..20f,
                                steps = 100
                            )
                        }
                        Row {
                            Text("ki:", Modifier.align(Alignment.CenterVertically).width(50.dp).padding(5.dp))
                            TextField(
                                String.format("%.2f", pidParameters.ki),
                                { pidParameters = pidParameters.copy(ki = it.toDouble()) },
                                Modifier.width(100.dp),
                                enabled = false
                            )

                            Slider(
                                pidParameters.ki.toFloat(),
                                { pidParameters = pidParameters.copy(ki = it.toDouble()) },
                                valueRange = -10f..10f,
                                steps = 100
                            )
                        }
                        Row {
                            Text("kd:", Modifier.align(Alignment.CenterVertically).width(50.dp).padding(5.dp))
                            TextField(
                                String.format("%.2f", pidParameters.kd),
                                { pidParameters = pidParameters.copy(kd = it.toDouble()) },
                                Modifier.width(100.dp),
                                enabled = false
                            )

                            Slider(
                                pidParameters.kd.toFloat(),
                                { pidParameters = pidParameters.copy(kd = it.toDouble()) },
                                valueRange = -10f..10f,
                                steps = 100
                            )
                        }

                        Row {
                            Text("dt:", Modifier.align(Alignment.CenterVertically).width(50.dp).padding(5.dp))
                            TextField(
                                pidParameters.timeStep.toString(DurationUnit.MILLISECONDS),
                                { pidParameters = pidParameters.copy(timeStep = it.toDouble().milliseconds) },
                                Modifier.width(100.dp),
                                enabled = false
                            )

                            Slider(
                                pidParameters.timeStep.toDouble(DurationUnit.MILLISECONDS).toFloat(),
                                { pidParameters = pidParameters.copy(timeStep = it.toDouble().milliseconds) },
                                valueRange = 1f..100f,
                                steps = 100
                            )
                        }
                        Row {
                            Button({
                                pidParameters = PidParameters(
                                    kp = 2.5,
                                    ki = 0.0,
                                    kd = -0.1,
                                    timeStep = 0.005.seconds
                                )
                            }) {
                                Text("Reset")
                            }
                        }
                    }
                }
                second(400.dp) {
                    ChartLayout {
                        XYGraph<Instant, Double>(
                            xAxisModel = remember { TimeAxisModel.recent(maxAge, clock) },
                            yAxisModel = rememberDoubleLinearAxisModel(state.range),
                            xAxisTitle = { Text("Time in seconds relative to current") },
                            xAxisLabels = { it: Instant ->
                                androidx.compose.material3.Text(
                                    (clock.now() - it).toDouble(
                                        DurationUnit.SECONDS
                                    ).toString(2)
                                )
                            },
                            yAxisLabels = { it: Double -> Text(it.toString(2)) }
                        ) {
                            PlotNumberState(
                                context = context,
                                state = state,
                                maxAge = maxAge,
                                sampling = 50.milliseconds,
                                lineStyle = LineStyle(SolidColor(Color.Blue))
                            )
                            PlotDeviceProperty(
                                linearDrive.drive,
                                Drive.position,
                                maxAge = maxAge,
                                sampling = 50.milliseconds,
                            )
                            PlotNumberState(
                                context = context,
                                state = linearDrive.pid.target,
                                maxAge = maxAge,
                                sampling = 50.milliseconds,
                                lineStyle = LineStyle(SolidColor(Color.Red))
                            )
                        }
                        Surface {
                            FlowLegend(3, label = {
                                when (it) {
                                    0 -> {
                                        Text("Body position", color = Color.Blue)
                                    }

                                    1 -> {
                                        Text("Regulator position", color = Color.Black)
                                    }

                                    2 -> {
                                        Text("Regulator target", color = Color.Red)
                                    }
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}