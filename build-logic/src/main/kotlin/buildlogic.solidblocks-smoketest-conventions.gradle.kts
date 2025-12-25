import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.register

plugins {
    id("org.jetbrains.kotlin.jvm")
}

sourceSets {
    create("smokeTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val smokeTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}
val smokeTestRuntimeOnly by configurations.getting

configurations.create("smokeTest").extendsFrom(configurations.runtimeOnly.get())

val integrationTest = tasks.register<Test>("smokeTest") {
    description = "Runs smoke tests."
    group = "verification"

    testClassesDirs = sourceSets["smokeTest"].output.classesDirs
    classpath = sourceSets["smokeTest"].runtimeClasspath

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
