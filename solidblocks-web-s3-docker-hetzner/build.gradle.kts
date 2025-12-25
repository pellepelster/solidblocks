plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("buildlogic.solidblocks-smoketest-conventions")
}

dependencies {
    testImplementation(project(":solidblocks-test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
    testImplementation("io.minio:minio:8.6.0")
    testImplementation("com.github.docker-java:docker-java:3.4.0")
    testImplementation("com.github.docker-java:docker-java-transport-zerodep:3.4.0")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")

    smokeTestImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    smokeTestRuntimeOnly("org.junit.platform:junit-platform-launcher")
    smokeTestImplementation(project(":solidblocks-test"))
    smokeTestImplementation("org.awaitility:awaitility-kotlin:4.2.0")
}