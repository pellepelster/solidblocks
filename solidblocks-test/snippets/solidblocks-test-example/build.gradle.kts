import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.10"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation("de.solidblocks:infra-test:0.4.14")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("24")
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "24"
    targetCompatibility = "24"
}
