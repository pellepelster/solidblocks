object Versions {
    const val junitJupiterVersion = "5.9.0"
    const val testContainersVersion = "1.17.1"
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.21" // <1>
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("io.github.microutils:kotlin-logging-jvm:2.0.6")

    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.junitJupiterVersion}")
    testImplementation("org.assertj:assertj-core:3.22.0")

    testImplementation("org.testcontainers:testcontainers:${Versions.testContainersVersion}")
    testImplementation("org.testcontainers:junit-jupiter:${Versions.testContainersVersion}")

    testImplementation("io.mockk:mockk:1.12.4")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")

    testImplementation("ch.qos.logback:logback-classic:1.2.10")

    testImplementation("org.postgresql:postgresql:42.4.1")
    testImplementation("io.github.hakky54:sslcontext-kickstart:7.4.5")
    testImplementation("io.github.hakky54:sslcontext-kickstart-for-pem:7.4.5")

    testImplementation("io.minio:minio:8.4.3")

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