plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.10"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    testImplementation("de.solidblocks:infra-test:0.4.12-rc3")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
