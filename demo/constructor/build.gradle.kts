import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("space.kscience.gradle.mpp")
    alias(spclibs.plugins.compose)
}

kscience {
    jvm {
        withJava()
    }
    useKtor()
    useSerialization()
    useContextReceivers()
    commonMain {
        implementation(projects.controlsVision)
        implementation(projects.controlsConstructor)
//        implementation("io.github.koalaplot:koalaplot-core:0.6.0")
    }
    jvmMain {
//        implementation("io.ktor:ktor-server-cio")
        implementation(spclibs.logback.classic)
    }
}

kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.desktop.components.splitPane)
            }
        }
    }
}

//application {
//    mainClass.set("space.kscience.controls.demo.constructor.MainKt")
//}

kotlin.explicitApi = ExplicitApiMode.Disabled


compose.desktop {
    application {
        mainClass = "space.kscience.controls.demo.constructor.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "PidConstructor"
            packageVersion = "1.0.0"
        }
    }
}