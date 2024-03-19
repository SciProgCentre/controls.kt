plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

description = """
   MQTT client magix endpoint
""".trimIndent()

dependencies {
    api(projects.magix.magixApi)
    implementation(libs.hivemq.mqtt.client)
    implementation(spclibs.kotlinx.coroutines.jdk8)
}

readme{
    maturity = space.kscience.gradle.Maturity.PROTOTYPE
}
