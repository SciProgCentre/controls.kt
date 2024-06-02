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
import space.kscience.controls.compose.NumberTextField
import space.kscience.controls.compose.PlotNumericState
import space.kscience.controls.compose.TimeAxisModel
import space.kscience.controls.constructor.DeviceConstructor
import space.kscience.controls.constructor.MutableDeviceState
import space.kscience.controls.constructor.devices.Drive
import space.kscience.controls.constructor.devices.LimitSwitch
import space.kscience.controls.constructor.devices.LinearDrive
import space.kscience.controls.constructor.models.Inertia
import space.kscience.controls.constructor.models.MutableRangeState
import space.kscience.controls.constructor.models.PidParameters
import space.kscience.controls.constructor.models.ScrewDrive
import space.kscience.controls.constructor.timer
import space.kscience.controls.constructor.units.Kilograms
import space.kscience.controls.constructor.units.Meters
import space.kscience.controls.constructor.units.NumericalValue
import space.kscience.controls.manager.ClockManager
import space.kscience.controls.manager.DeviceManager
import space.kscience.controls.manager.clock
import space.kscience.controls.manager.install
import space.kscience.dataforge.context.Context
import java.awt.Dimension
import kotlin.math.PI
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit


class Modulator(
    context: Context,
    target: MutableDeviceState<NumericalValue<Meters>>,
    var freq: Double = 0.1,
    var timeStep: Duration = 5.milliseconds,
) : DeviceConstructor(context) {
    private val clockStart = clock.now()

    private val modulation = timer(10.milliseconds).onNext {
        val timeFromStart = clock.now() - clockStart
        val t = timeFromStart.toDouble(DurationUnit.SECONDS)
        target.value = NumericalValue(
            5 * sin(2.0 * PI * freq * t) +
                    sin(2 * PI * 21 * freq * t + 0.02 * (timeFromStart / timeStep))
        )
    }
}


private val inertia = NumericalValue<Kilograms>(0.1)

private val leverage = NumericalValue<Meters>(0.05)

private val maxAge = 10.seconds

private val range = -6.0..6.0

/**
 * The whole physical model is here
 */
private fun createLinearDriveModel(context: Context, pidParameters: PidParameters): LinearDrive {

    //create a drive model with zero starting force
    val drive = Drive(context)

    //a screw drive to converse a rotational moment into a linear one
    val screwDrive = ScrewDrive(context, leverage)

    // Create a physical position coerced in a given range
    val position = MutableRangeState<Meters>(0.0, range)

    /**
     * Create an inertia model.
     * The inertia uses drive force as input. Position is used as both input and output
     *
     * Force is the input parameter, position is output parameter
     *
     */
    val inertia = Inertia.linear(
        context = context,
        force = screwDrive.transformForce(drive.force),
        mass = inertia,
        position = position
    )

    /**
     * Create a limit switches from physical position
     */
    val startLimitSwitch = LimitSwitch(context, position.atStart)
    val endLimitSwitch = LimitSwitch(context, position.atEnd)

    return context.install(
        "linearDrive",
        LinearDrive(drive, startLimitSwitch, endLimitSwitch, position, pidParameters)
    )
}


private fun createModulator(linearDrive: LinearDrive): Modulator = linearDrive.context.install(
    "modulator",
    Modulator(linearDrive.context, linearDrive.pid.target)
)

@OptIn(ExperimentalSplitPaneApi::class, ExperimentalKoalaPlotApi::class)
fun main() = application {
    val context = remember {
        Context {
            plugin(DeviceManager)
            plugin(ClockManager)
        }
    }

    var pidParameters by remember {
        mutableStateOf(PidParameters(kp = 900.0, ki = 20.0, kd = -50.0, timeStep = 0.005.seconds))
    }

    val linearDrive: LinearDrive = remember {
        createLinearDriveModel(context, pidParameters)
    }

    val modulator = remember {
        createModulator(linearDrive)
    }

    //bind pid parameters
    LaunchedEffect(Unit) {
        snapshotFlow {
            pidParameters
        }.onEach {
            linearDrive.pid.pidParameters = pidParameters
        }.collect()
    }

    val clock = remember { context.clock }

    Window(title = "Pid regulator simulator", onCloseRequest = ::exitApplication) {
        window.minimumSize = Dimension(800, 400)
        MaterialTheme {
            HorizontalSplitPane {
                first(400.dp) {
                    Column(modifier = Modifier.background(color = Color.LightGray).fillMaxHeight()) {
                        Row {
                            Text("kp:", Modifier.align(Alignment.CenterVertically).width(50.dp).padding(5.dp))
                            NumberTextField(
                                value = pidParameters.kp,
                                onValueChange = { pidParameters = pidParameters.copy(kp = it.toDouble()) },
                                formatter = { String.format("%.2f", it.toDouble()) },
                                step = 1.0,
                                modifier = Modifier.width(200.dp),
                            )
                            Slider(
                                pidParameters.kp.toFloat(),
                                { pidParameters = pidParameters.copy(kp = it.toDouble()) },
                                valueRange = 0f..1000f,
                                steps = 100
                            )
                        }
                        Row {
                            Text("ki:", Modifier.align(Alignment.CenterVertically).width(50.dp).padding(5.dp))
                            NumberTextField(
                                value = pidParameters.ki,
                                onValueChange = { pidParameters = pidParameters.copy(ki = it.toDouble()) },
                                formatter = { String.format("%.2f", it.toDouble()) },
                                step = 0.1,
                                modifier = Modifier.width(200.dp),
                            )

                            Slider(
                                pidParameters.ki.toFloat(),
                                { pidParameters = pidParameters.copy(ki = it.toDouble()) },
                                valueRange = -100f..100f,
                                steps = 100
                            )
                        }
                        Row {
                            Text("kd:", Modifier.align(Alignment.CenterVertically).width(50.dp).padding(5.dp))
                            NumberTextField(
                                value = pidParameters.kd,
                                onValueChange = { pidParameters = pidParameters.copy(kd = it.toDouble()) },
                                formatter = { String.format("%.2f", it.toDouble()) },
                                step = 0.1,
                                modifier = Modifier.width(200.dp),
                            )

                            Slider(
                                pidParameters.kd.toFloat(),
                                { pidParameters = pidParameters.copy(kd = it.toDouble()) },
                                valueRange = -100f..100f,
                                steps = 100
                            )
                        }

                        Row {
                            Text("dt:", Modifier.align(Alignment.CenterVertically).width(50.dp).padding(5.dp))
                            TextField(
                                pidParameters.timeStep.toString(DurationUnit.MILLISECONDS),
                                { pidParameters = pidParameters.copy(timeStep = it.toDouble().milliseconds) },
                                Modifier.width(200.dp),
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
                            yAxisModel = rememberDoubleLinearAxisModel((range.start - 1.0)..(range.endInclusive + 1.0)),
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
                            PlotNumericState(
                                context = context,
                                state = linearDrive.position,
                                maxAge = maxAge,
                                sampling = 50.milliseconds,
                                lineStyle = LineStyle(SolidColor(Color.Blue))
                            )
                            PlotNumericState(
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