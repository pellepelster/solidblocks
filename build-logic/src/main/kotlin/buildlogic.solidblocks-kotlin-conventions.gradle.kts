plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.diffplug.spotless")
}

object Versions {
    const val junitJupiterVersion = "5.11.0"
    const val testContainersVersion = "1.17.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.junitJupiterVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junitJupiterVersion}")

}

tasks.test {
    useJUnitPlatform()
    testLogging {
        this.showStandardStreams = true
        events = setOf(
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT,
            org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
        )
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

spotless {
    kotlin {
        ktlint()
        ktfmt()
    }
}