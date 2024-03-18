import space.kscience.gradle.Maturity

plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

description = """
    Common utilities and services for Magix endpoints.   
""".trimIndent()

val dataforgeVersion: String by rootProject.extra

kscience {
    jvm()
    js()
    native()
    useSerialization()
    commonMain {
        api(projects.magix.magixApi)
        api("space.kscience:dataforge-meta:$dataforgeVersion")
    }
}

readme {
    maturity = Maturity.EXPERIMENTAL
}