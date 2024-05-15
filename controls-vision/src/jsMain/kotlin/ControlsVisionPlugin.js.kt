package space.kscience.controls.vision

import kotlinx.serialization.modules.SerializersModule
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.visionforge.VisionPlugin
import space.kscience.visionforge.html.ElementVisionRenderer

private val indicatorRenderer = ElementVisionRenderer<BooleanIndicatorVision> { name, vision: BooleanIndicatorVision, meta ->

}


public actual class ControlVisionPlugin : VisionPlugin() {
    override val tag: PluginTag get() = Companion.tag

    override val visionSerializersModule: SerializersModule get() = controlsVisionSerializersModule

    override fun content(target: String): Map<Name, Any> = when (target) {
        ElementVisionRenderer.TYPE -> mapOf(
            "indicator".asName() to indicatorRenderer
        )

        else -> super.content(target)
    }

    public actual companion object : PluginFactory<ControlVisionPlugin> {
        override val tag: PluginTag = PluginTag("controls.vision")

        override fun build(context: Context, meta: Meta): ControlVisionPlugin = ControlVisionPlugin()

    }
}