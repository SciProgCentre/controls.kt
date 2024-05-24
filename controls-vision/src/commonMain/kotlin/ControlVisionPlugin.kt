package space.kscience.controls.vision

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import space.kscience.dataforge.context.PluginFactory
import space.kscience.visionforge.Vision
import space.kscience.visionforge.VisionPlugin

public expect class ControlVisionPlugin: VisionPlugin{
    public companion object: PluginFactory<ControlVisionPlugin>
}

internal val controlsVisionSerializersModule = SerializersModule {
    polymorphic(Vision::class) {
        subclass(IndicatorVision.serializer())
        subclass(SliderVision.serializer())
    }
}