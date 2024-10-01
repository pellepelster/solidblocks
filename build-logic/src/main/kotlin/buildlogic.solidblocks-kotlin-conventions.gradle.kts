import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.io.ByteArrayOutputStream

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
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

fun getPassCredential(passPath: String, envName: String): String {
    if (System.getenv(envName) != null) {
        return System.getenv(envName)!!
    }

    val secret = ByteArrayOutputStream()
    exec {
        commandLine("pass", passPath)
        standardOutput = secret
    }

    return secret.toString().trim()
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