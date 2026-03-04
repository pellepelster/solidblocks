plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.10"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation("de.solidblocks:hetzner-cloud:0.0.0")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
