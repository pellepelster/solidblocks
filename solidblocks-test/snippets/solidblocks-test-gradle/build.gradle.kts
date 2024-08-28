plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    implementation("de.solidblocks:infra-test:v0.2.7")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
