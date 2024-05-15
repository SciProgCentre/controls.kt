package space.kscience.controls.vision

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import space.kscience.dataforge.meta.boolean
import space.kscience.visionforge.AbstractVision
import space.kscience.visionforge.Vision
import space.kscience.visionforge.html.VisionOfHtml

/**
 * A [Vision] that shows an indicator
 */
@Serializable
@SerialName("controls.indicator")
public class BooleanIndicatorVision : AbstractVision(), VisionOfHtml {
    public val isOn: Boolean by properties.boolean(false)
}

///**
// * A [Vision] that allows both showing the value and changing it
// */
//public interface RegulatorVision: IndicatorVision{
//
//}