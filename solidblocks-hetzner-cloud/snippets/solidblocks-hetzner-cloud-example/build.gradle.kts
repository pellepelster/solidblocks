plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.10"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("de.solidblocks:hetzner-cloud:0.4.13")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
