import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("org.jetbrains.kotlin.jvm")
}

sourceSets {
    create("releaseTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val releaseTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}
val releaseTestRuntimeOnly by configurations.getting

configurations.create("releaseTest").extendsFrom(configurations.runtimeOnly.get())

val integrationTest = tasks.register<Test>("releaseTest") {
    description = "Runs release tests."
    group = "verification"

    testClassesDirs = sourceSets["releaseTest"].output.classesDirs
    classpath = sourceSets["releaseTest"].runtimeClasspath

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

dependencies {
    releaseTestImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    releaseTestRuntimeOnly("org.junit.platform:junit-platform-launcher")
}