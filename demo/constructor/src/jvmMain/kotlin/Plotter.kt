package space.kscience.controls.demo.constructor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import space.kscience.controls.constructor.DeviceConstructor
import space.kscience.controls.constructor.MutableDeviceState
import space.kscience.controls.constructor.device
import space.kscience.controls.constructor.devices.StepDrive
import space.kscience.controls.constructor.devices.angle
import space.kscience.controls.constructor.models.RangeState
import space.kscience.controls.constructor.models.ScrewDrive
import space.kscience.controls.constructor.models.coerceIn
import space.kscience.controls.constructor.units.*
import space.kscience.controls.manager.ClockManager
import space.kscience.controls.manager.DeviceManager
import space.kscience.dataforge.context.Context
import java.awt.Dimension
import kotlin.random.Random


class Plotter(
    context: Context,
    xDrive: StepDrive,
    yDrive: StepDrive,
    val paint: suspend (Color) -> Unit,
) : DeviceConstructor(context) {
    val xDrive by device(xDrive)
    val yDrive by device(yDrive)

    public fun moveToXY(x: Int, y: Int) {
        xDrive.target.value = x
        yDrive.target.value = y
    }

    //TODO add calibration

    // TODO add draw as action
}

suspend fun Plotter.modernArt(xRange: IntRange, yRange: IntRange) {
    while (isActive){
        val randomX = Random.nextInt(xRange.first, xRange.last)
        val randomY = Random.nextInt(xRange.first, xRange.last)
        moveToXY(randomX, randomY)
        delay(500)
        paint(Color(Random.nextInt()))
    }
}

suspend fun Plotter.square(xRange: IntRange, yRange: IntRange) {
    while (isActive) {
        moveToXY(xRange.first, yRange.first)
        delay(1000)
        paint(Color.Red)

        moveToXY(xRange.first, yRange.last)
        delay(1000)
        paint(Color.Red)

        moveToXY(xRange.last, yRange.last)
        delay(1000)
        paint(Color.Red)

        moveToXY(xRange.last, yRange.first)
        delay(1000)
        paint(Color.Red)
    }
}

private val xRange = NumericalValue<Meters>(-0.5)..NumericalValue<Meters>(0.5)
private val yRange = NumericalValue<Meters>(-0.5)..NumericalValue<Meters>(0.5)
private val ticksPerSecond = MutableDeviceState(250.0)
private val step = NumericalValue<Degrees>(1.2)


private data class PlotterPoint(
    val x: NumericalValue<Meters>,
    val y: NumericalValue<Meters>,
    val color: Color = Color.Black,
)

suspend fun main() = application {
    Window(title = "Pid regulator simulator", onCloseRequest = ::exitApplication) {
        window.minimumSize = Dimension(400, 400)

        val points = remember { mutableStateListOf<PlotterPoint>() }
        var position by remember { mutableStateOf(PlotterPoint(NumericalValue(0), NumericalValue(0))) }

        LaunchedEffect(Unit) {
            val context = Context {
                plugin(DeviceManager)
                plugin(ClockManager)
            }

            /* Here goes the device definition block */


            val xScrewDrive = ScrewDrive(context, NumericalValue(0.01))
            val xDrive = StepDrive(context, ticksPerSecond)
            val x: RangeState<NumericalValue<Meters>> = xScrewDrive.transformOffset(xDrive.angle(step)).coerceIn(xRange)

            val yScrewDrive = ScrewDrive(context, NumericalValue(0.01))
            val yDrive = StepDrive(context, ticksPerSecond)
            val y: RangeState<NumericalValue<Meters>> = yScrewDrive.transformOffset(yDrive.angle(step)).coerceIn(yRange)

            val plotter = Plotter(context, xDrive, yDrive) { color ->
                println("Point X: ${x.value.value}, Y: ${y.value.value}, color: $color")
                points.add(PlotterPoint(x.value, y.value, color))
            }


            /* Start visualization program */

            launch {
                x.valueFlow.collect {
                    position = position.copy(x = it)
                }
            }

            launch {
                y.valueFlow.collect {
                    position = position.copy(y = it)
                }
            }

            launch {
                val range = -100..100
                plotter.modernArt(range, range)
                //plotter.square(range, range)
            }
        }


        /* Here goes the visualization block */

        MaterialTheme {
            Canvas(modifier = Modifier.fillMaxSize()) {
                fun toOffset(x: NumericalValue<Meters>, y: NumericalValue<Meters>): Offset {
                    val canvasX = (x - xRange.start) / (xRange.endInclusive - xRange.start) * size.width
                    val canvasY = (y - yRange.start) / (yRange.endInclusive - yRange.start) * size.height
                    return Offset(canvasX.toFloat(), canvasY.toFloat())
                }

                val center = toOffset(position.x, position.y)


                drawRect(
                    Color.LightGray,
                    topLeft = Offset(0f, center.y - 5f),
                    size = Size(size.width, 10f)
                )

                drawCircle(Color.Black, radius = 10f, center = center)


                points.forEach {
                    drawCircle(it.color, radius = 2f, center = toOffset(it.x, it.y))
                }
            }
        }
    }

}