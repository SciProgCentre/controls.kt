plugins {
    id("ru.mipt.npm.jvm")
    id("ru.mipt.npm.publish")
}

dependencies{
    api(project(":dataforge-device-core"))
    implementation("org.scream3r:jssc:2.8.0")
}