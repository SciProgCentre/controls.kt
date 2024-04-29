import space.kscience.gradle.Maturity

plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

description = """
    Implementation of byte ports on top os ktor-io asynchronous API
""".trimIndent()

dependencies {
    api(projects.controlsCore)
    api(spclibs.ktor.network)
}

readme{
    maturity = Maturity.PROTOTYPE
}
