import space.kscience.gradle.Maturity

plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

description = """
    Magix service for binding controls devices (both as RPC client and server)
""".trimIndent()

kscience {
    jvm()
    js()
    native()
    useCoroutines()
    useSerialization {
        json()
    }
    commonMain {
        api(projects.magix.magixApi)
        api(projects.controlsCore)
        api(libs.uuid)
    }

    jvmTest{
        implementation(spclibs.logback.classic)
    }
}

readme {
    maturity = Maturity.EXPERIMENTAL

    feature("controlsMagix", ref = "src/commonMain/kotlin/space/kscience/controls/client/controlsMagix.kt"){
        """
            Connect a `DeviceManage` with one or many devices to the Magix endpoint
        """.trimIndent()
    }

    feature("DeviceClient", ref = "src/commonMain/kotlin/space/kscience/controls/client/DeviceClient.kt"){
        """
            A remote connector to Controls-kt device via Magix
        """.trimIndent()
    }
}