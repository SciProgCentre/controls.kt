plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

kscience {
    useCoroutines()
}

dependencies {
    api(projects.magix.magixStorage)
    implementation(libs.xodus.entity.store)
//    implementation("org.jetbrains.xodus:dnq:2.0.0")

    testImplementation(spclibs.kotlinx.coroutines.test)
}

readme {
    maturity = space.kscience.gradle.Maturity.PROTOTYPE
}
