import space.kscience.gradle.Maturity

plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

val plc4xVersion = "0.12.0"

description = """
    A plugin for Controls-kt device server on top of plc4x library
""".trimIndent()

kscience{
    jvm()
    jvmMain{
        api(projects.controlsCore)
        api("org.apache.plc4x:plc4j-spi:$plc4xVersion")
    }
}

readme{
    maturity = Maturity.EXPERIMENTAL
}