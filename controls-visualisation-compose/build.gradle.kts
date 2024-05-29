import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    id("space.kscience.gradle.mpp")
    alias(spclibs.plugins.compose)
    `maven-publish`
}

description = """
    Visualisation extension using compose-multiplatform
""".trimIndent()

kscience {
    jvm()
    useKtor()
    useSerialization()
    useContextReceivers()
    commonMain {
        api(projects.controlsConstructor)
        api("io.github.koalaplot:koalaplot-core:0.6.0")
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(compose.foundation)
                api(compose.material3)
                @OptIn(ExperimentalComposeLibrary::class)
                api(compose.desktop.components.splitPane)
            }
        }
//        jvmMain {
//            dependencies {
//                implementation(compose.desktop.currentOs)
//            }
//        }
    }
}


readme {
    maturity = space.kscience.gradle.Maturity.PROTOTYPE
}