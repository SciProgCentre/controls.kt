plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

description = """
    Utils to work with controls-kt on Raspberry pi
""".trimIndent()

dependencies{
    api(project(":controls-core"))
    api(libs.pi4j.ktx) // Kotlin DSL
    api(libs.pi4j.core)
    api(libs.pi4j.plugin.raspberrypi)
    api(libs.pi4j.plugin.pigpio)
}