package space.kscience.controls.spec

import kotlinx.datetime.Instant
import space.kscience.dataforge.meta.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

public fun Double.asMeta(): Meta = Meta(asValue())

/**
 * Generate a nullable [MetaConverter] from non-nullable one
 */
public fun <T : Any> MetaConverter<T>.nullable(): MetaConverter<T?> = object : MetaConverter<T?> {
    override fun convert(obj: T?): Meta  = obj?.let { this@nullable.convert(it) }?: Meta(Null)

    override fun readOrNull(source: Meta): T? = if(source.value == Null) null else this@nullable.readOrNull(source)

}

//TODO to be moved to DF
private object DurationConverter : MetaConverter<Duration> {
    override fun readOrNull(source: Meta): Duration = source.value?.double?.toDuration(DurationUnit.SECONDS)
        ?: run {
            val unit: DurationUnit = source["unit"].enum<DurationUnit>() ?: DurationUnit.SECONDS
            val value = source[Meta.VALUE_KEY].double ?: error("No value present for Duration")
            return@run value.toDuration(unit)
        }

    override fun convert(obj: Duration): Meta = obj.toDouble(DurationUnit.SECONDS).asMeta()
}

public val MetaConverter.Companion.duration: MetaConverter<Duration> get() = DurationConverter


private object InstantConverter : MetaConverter<Instant> {
    override fun readOrNull(source: Meta): Instant? = source.string?.let { Instant.parse(it) }
    override fun convert(obj: Instant): Meta = Meta(obj.toString())
}

public val MetaConverter.Companion.instant: MetaConverter<Instant> get() = InstantConverter