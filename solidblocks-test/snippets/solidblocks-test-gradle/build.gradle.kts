plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.10"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testImplementation("de.solidblocks:infra-test:v0.4.10")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
