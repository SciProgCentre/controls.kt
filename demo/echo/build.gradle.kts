plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
    maven("https://repo.kotlin.link")
}

dependencies {
    implementation(projects.magix.magixServer)
    implementation(projects.magix.magixRsocket)
    implementation(projects.magix.magixZmq)
    implementation(spclibs.ktor.client.cio)

    implementation(libs.logback.classic)
}
kotlin{
    jvmToolchain(11)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf("-Xjvm-default=all", "-Xopt-in=kotlin.RequiresOptIn")
    }
}

application {
    mainClass.set("space.kscience.controls.demo.echo.MainKt")
}