package space.kscience.controls.demo.constructor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.serialization.Serializable
import space.kscience.controls.constructor.*
import space.kscience.dataforge.context.Context
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

@Serializable
data class XY(val x: Double, val y: Double) {
    companion object {
        val ZERO = XY(0.0, 0.0)
    }
}

operator fun XY.plus(other: XY): XY = XY(x + other.x, y + other.y)
operator fun XY.times(c: Double): XY = XY(x * c, y * c)
operator fun XY.div(c: Double): XY = XY(x / c, y / c)
//
//class XYPosition(context: Context, x0: Double, y0: Double) : DeviceModel(context) {
//    val x: MutableDeviceState<Double> = mutableState(x0)
//    val y: MutableDeviceState<Double> = mutableState(y0)
//
//    val xy = combineState(x, y) { x, y -> XY(x, y) }
//}

class Spring(
    context: Context,
    val k: Double,
    val l0: Double,
    val begin: DeviceState<XY>,
    val end: DeviceState<XY>,
) : DeviceConstructor(context) {

    val length = combineState(begin, end) { begin, end ->
        sqrt((end.y - begin.y).pow(2) + (end.x - begin.x).pow(2))
    }

    val tension: DeviceState<Double> = mapState(length) { l ->
        val delta = l - l0
        k * delta
    }

    /**
     * direction from start to end
     */
    val direction = combineState(begin, end) { begin, end ->
        val dx = end.x - begin.x
        val dy = end.y - begin.y
        val l = sqrt((end.y - begin.y).pow(2) + (end.x - begin.x).pow(2))
        XY(dx / l, dy / l)
    }

    val beginForce = combineState(direction, tension) { direction: XY, tension: Double ->
        direction * (tension)
    }


    val endForce = combineState(direction, tension) { direction: XY, tension: Double ->
        direction * (-tension)
    }
}

class MaterialPoint(
    context: Context,
    val mass: Double,
    val force: DeviceState<XY>,
    val position: MutableDeviceState<XY>,
    val velocity: MutableDeviceState<XY> = MutableDeviceState(XY.ZERO),
) : DeviceModel(context, force) {

    private val timer: TimerState = timer(2.milliseconds)

    private val movement = timer.onChange(
        position, velocity,
        alsoReads = setOf(force, velocity, position)
    ) { prev, next ->
        val dt = (next - prev).toDouble(DurationUnit.SECONDS)
        val a = force.value / mass
        position.value += a * (dt * dt / 2) + velocity.value * dt
        velocity.value += a * dt
    }
}


class BodyOnSprings(
    context: Context,
    mass: Double,
    k: Double,
    startPosition: XY,
    l0: Double = 1.0,
    val xLeft: Double = 0.0,
    val xRight: Double = 2.0,
    val yBottom: Double = 0.0,
    val yTop: Double = 2.0,
) : DeviceConstructor(context) {

    val width = xRight - xLeft
    val height = yTop - yBottom

    val position = mutableState(startPosition)

    private val leftAnchor = mutableState(XY(xLeft, yTop + yBottom / 2))

    val leftSpring by device(
        Spring(context, k, l0, leftAnchor, position)
    )

    private val rightAnchor = mutableState(XY(xRight, yTop + yBottom / 2))

    val rightSpring by device(
        Spring(context, k, l0, rightAnchor, position)
    )

    val force: DeviceState<XY> = combineState(leftSpring.endForce, rightSpring.endForce) { left, rignt ->
        left + rignt
    }


    val body = model(
        MaterialPoint(
            context = context,
            mass = mass,
            force = force,
            position = position
        )
    )
}

@Composable
fun <T> DeviceState<T>.collect(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
): State<T> = valueFlow.collectAsState(value, coroutineContext)

fun main() = application {
    val initialState = XY(1.1, 1.1)

    Window(title = "Ball on springs", onCloseRequest = ::exitApplication) {
        MaterialTheme {
            val context = remember {
                Context("simulation")
            }

            val model = remember {
                BodyOnSprings(context, 100.0, 1000.0, initialState)
            }

            val position: XY by model.body.position.collect()
            Box(Modifier.size(400.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    fun XY.toOffset() = Offset(
                        (x / model.width * size.width).toFloat(),
                        (y / model.height * size.height).toFloat()
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