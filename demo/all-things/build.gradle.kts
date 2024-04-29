plugins {
    kotlin("jvm")
    id("org.openjfx.javafxplugin") version "0.0.13"
    application
}


repositories {
    mavenCentral()
    maven("https://repo.kotlin.link")
}

dependencies {
    implementation(projects.controlsCore)
    //implementation(projects.controlsServer)
    implementation(projects.magix.magixServer)
    implementation(projects.controlsMagix)
    implementation(projects.magix.magixRsocket)
    implementation(projects.magix.magixZmq)
    implementation(projects.controlsOpcua)

    implementation(spclibs.ktor.client.cio)
    implementation(libs.tornadofx)
    implementation(libs.plotlykt.server)
//    implementation("com.github.Ricky12Awesome:json-schema-serialization:0.6.6")
    implementation(spclibs.logback.classic)
}

kotlin{
    jvmToolchain(11)
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf("-Xjvm-default=all", "-Xopt-in=kotlin.RequiresOptIn")
    }
}

javafx {
    version = "17"
    modules("javafx.controls")
}

application {
    mainClass.set("space.kscience.controls.demo.DemoControllerViewKt")
}