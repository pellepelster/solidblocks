object Versions {
    const val junitJupiterVersion = "5.11.0-RC1"
    const val testContainersVersion = "1.17.1"
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("io.kotest:kotest-assertions-core:5.9.1")
    implementation("com.github.docker-java:docker-java:3.4.0")
    implementation("com.github.docker-java:docker-java-transport-zerodep:3.4.0")

    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.junitJupiterVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junitJupiterVersion}")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("org.slf4j:slf4j-simple:2.0.13")
}

tasks.test {
    useJUnitPlatform()
}
