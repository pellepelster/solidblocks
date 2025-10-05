import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("org.jetbrains.kotlin.jvm")
    //id("com.diffplug.spotless")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.12.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")

    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-assertions-json:5.9.1")
    testImplementation("io.kotest:kotest-property:5.9.1")
    testImplementation("io.mockk:mockk:1.14.3")
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
            "HETZNER_DNS_API_TOKEN" to providers.of(PassSecretValueSource::class) {
                this.parameters.path.set("solidblocks/hetzner/test/dns_api_token")
                this.parameters.environment.set("HETZNER_DNS_API_TOKEN")
            }.get(),
            "HCLOUD_TOKEN" to providers.of(PassSecretValueSource::class) {
                this.parameters.path.set("solidblocks/hetzner/test/hcloud_api_token")
                this.parameters.environment.set("HCLOUD_TOKEN")
            }.get(),
            "GCP_SERVICE_ACCOUNT_KEY" to providers.of(PassSecretValueSource::class) {
                this.parameters.path.set("solidblocks/gcp/test/service_account_key")
                this.parameters.environment.set("GCP_SERVICE_ACCOUNT_KEY")
            }.get()
        )
    )
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

/*
spotless {
    kotlin {
        ktlint()
        ktfmt()
    }
}
*/