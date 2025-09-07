import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.diffplug.spotless")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")

    testImplementation("io.kotest:kotest-runner-junit5:5.9.0")
    testImplementation("io.kotest:kotest-assertions-core:5.9.0")
    testImplementation("io.kotest:kotest-assertions-json:5.9.0")
    testImplementation("io.kotest:kotest-property:5.9.0")
    testImplementation("io.mockk:mockk:1.13.11")
}

fun getPassCredential(passPath: String, envName: String): String {
    if (System.getenv(envName) != null) {
        return System.getenv(envName)!!
    }

    /*
    val secret = ByteArrayOutputStream()
    exec {
        commandLine("pass", passPath)
        standardOutput = secret
    }

    return secret.toString().trim()

     */
    return "yolo"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        this.showStandardStreams = true
        events = setOf(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR
        )
    }

    environment(
        mapOf(
            "HETZNER_DNS_API_TOKEN" to getPassCredential(
                "solidblocks/hetzner/test/dns_api_token",
                "HETZNER_DNS_API_TOKEN"
            )
        )
    )

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
