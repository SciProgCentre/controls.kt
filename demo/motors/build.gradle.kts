plugins {
    id("space.kscience.gradle.jvm")
    alias(spclibs.plugins.compose)
}

//application{
//    mainClass.set("ru.mipt.npm.devices.pimotionmaster.PiMotionMasterAppKt")
//}

kotlin{
    explicitApi = null
}

val ktorVersion: String by rootProject.extra
val dataforgeVersion: String by extra

dependencies {
    implementation(project(":controls-ports-ktor"))
    implementation(projects.controlsMagix)
}
