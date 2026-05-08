plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.10"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("de.solidblocks:hetzner-cloud:0.0.0")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
