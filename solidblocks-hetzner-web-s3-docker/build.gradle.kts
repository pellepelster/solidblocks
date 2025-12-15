plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
}

dependencies {
    testImplementation(project(":solidblocks-test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
    testImplementation("io.minio:minio:8.6.0")
}