package space.kscience.controls.demo.constructor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.serialization.Serializable
import space.kscience.controls.compose.asComposeState
import space.kscience.controls.constructor.*
import space.kscience.dataforge.context.Context
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

@Serializable
private data class XY(val x: Double, val y: Double) {
    companion object {
        val ZERO = XY(0.0, 0.0)
    }
}

private val XY.length: Double get() = sqrt(x.pow(2) + y.pow(2))

private operator fun XY.plus(other: XY): XY = XY(x + other.x, y + other.y)
private operator fun XY.times(c: Double): XY = XY(x * c, y * c)
private operator fun XY.div(c: Double): XY = XY(x / c, y / c)

private class Spring(
    context: Context,
    val k: Double,
    val l0: Double,
    val begin: DeviceState<XY>,
    val end: DeviceState<XY>,
) : ModelConstructor(context) {

    /**
     * vector from start to end
     */
    val direction = combineState(begin, end) { begin: XY, end: XY ->
        val dx = end.x - begin.x
        val dy = end.y - begin.y
        val l = sqrt(dx.pow(2) + dy.pow(2))
        XY(dx / l, dy / l)
    }

    val tension: DeviceState<Double> = combineState(begin, end) { begin: XY, end: XY ->
        val dx = end.x - begin.x
        val dy = end.y - begin.y
        k * sqrt(dx.pow(2) + dy.pow(2))
    }


    val beginForce = combineState(direction, tension) { direction: XY, tension: Double ->
        direction * (tension)
    }


    val endForce = combineState(direction, tension) { direction: XY, tension: Double ->
        direction * (-tension)
    }
}

private class MaterialPoint(
    context: Context,
    val mass: Double,
    val force: DeviceState<XY>,
    val position: MutableDeviceState<XY>,
    val velocity: MutableDeviceState<XY> = MutableDeviceState(XY.ZERO),
) : ModelConstructor(context, force, position, velocity) {

    private val timer: TimerState = timer(2.milliseconds)

    //TODO synchronize force change

    private val movement = timer.onChange(
        writes = setOf(position, velocity),
        reads = setOf(force, velocity, position)
    ) { prev, next ->
        val dt = (next - prev).toDouble(DurationUnit.SECONDS)
        val a = force.value / mass
        position.value += a * (dt * dt / 2) + velocity.value * dt
        velocity.value += a * dt
    }
}


private class BodyOnSprings(
    context: Context,
    mass: Double,
    k: Double,
    startPosition: XY,
    l0: Double = 1.0,
    val xLeft: Double = -1.0,
    val xRight: Double = 1.0,
    val yBottom: Double = -1.0,
    val yTop: Double = 1.0,
) : DeviceConstructor(context) {

    val width = xRight - xLeft
    val height = yTop - yBottom

    val position = stateOf(startPosition)

    private val leftAnchor = stateOf(XY(xLeft, (yTop + yBottom) / 2))

    val leftSpring = model(
        Spring(context, k, l0, leftAnchor, position)
    )

    private val rightAnchor = stateOf(XY(xRight, (yTop + yBottom) / 2))

    val rightSpring = model(
        Spring(context, k, l0, rightAnchor, position)
    )

    val force: DeviceState<XY> = combineState(leftSpring.endForce, rightSpring.endForce) { left, right ->
        left + right
    }


    val body = model(
        MaterialPoint(
            context = context,
            mass = mass,
            force = force,
            position = position,
        )
    )
}

fun main() = application {
    val initialState = XY(0.1, 0.2)

    Window(title = "Ball on springs", onCloseRequest = ::exitApplication) {
        MaterialTheme {
            val context = remember {
                Context("simulation")
            }

            val model = remember {
                BodyOnSprings(context, 100.0, 1000.0, initialState)
            }

            //TODO add ability to freeze model

//            LaunchedEffect(Unit){
//                model.position.valueFlow.onEach {
//                    model.position.value = it.copy(y = model.position.value.y.coerceIn(-1.0..1.0))
//                }.collect()
//            }

            val position: XY by model.body.position.asComposeState()
            Box(Modifier.size(400.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    fun XY.toOffset() = Offset(
                        center.x + (x / model.width * size.width).toFloat(),
                        center.y - (y / model.height * size.height).toFloat()
                    )

                    drawCircle(
                        Color.Red, 10f, center = position.toOffset()
                    )
                    drawLine(Color.Blue, model.leftSpring.begin.value.toOffset(), model.leftSpring.end.value.toOffset())
                    drawLine(
                        Color.Blue,
                        model.rightSpring.begin.value.toOffset(),
                        model.rightSpring.end.value.toOffset()
                    )
                }
            }
        }
    }
}