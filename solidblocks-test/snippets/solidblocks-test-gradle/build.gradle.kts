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
    testImplementation("de.solidblocks:infra-test:v0.4.11-rc2")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
