import org.gradle.api.tasks.testing.logging.TestLogEvent

object Versions {
    const val testContainersVersion = "1.17.1"
}

plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.github.microutils:kotlin-logging-jvm:2.0.6")

    testImplementation("org.assertj:assertj-core:3.22.0")

    testImplementation("org.testcontainers:testcontainers:${Versions.testContainersVersion}")

    testImplementation("org.eclipse:yasson:1.0.1")
    testImplementation("org.glassfish:javax.json:1.1.2")

    testImplementation("io.minio:minio:8.5.17")
    testImplementation("com.squareup.okhttp3:okhttp-tls:4.11.0")
    testImplementation("io.github.hakky54:sslcontext-kickstart-for-pem:7.4.5")

    testImplementation("io.mockk:mockk:1.12.4")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")

    testImplementation("ch.qos.logback:logback-classic:1.4.12")
    testImplementation("org.postgresql:postgresql:42.7.2")
    testImplementation("org.jdbi:jdbi3-core:3.32.0")

    testImplementation("com.amazonaws:aws-java-sdk-s3:1.12.455")
    testImplementation("com.google.cloud:google-cloud-storage:2.34.0")
}

tasks.withType<Test> {
    useJUnitPlatform()

    systemProperties["junit.jupiter.execution.parallel.enabled"] = "true"
    systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
    systemProperties["junit.jupiter.execution.parallel.config.strategy"] = "fixed"
    systemProperties["junit.jupiter.execution.parallel.config.fixed.parallelism"] = 6

    testLogging {
        events = setOf(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR
        )
        showStandardStreams = true
        showStackTraces = true
    }

    environment(
        mapOf(
            "HETZNER_S3_ACCESS_KEY" to providers.of(PassSecretValueSource::class) {
                this.parameters.passName.set("solidblocks/hetzner/test/s3_access_key_id")
            }.get(),
            "HETZNER_S3_SECRET_KEY" to providers.of(PassSecretValueSource::class) {
                this.parameters.passName.set("solidblocks/hetzner/test/s3_secret_key")
            }.get(),
            "AWS_ACCESS_KEY_ID" to providers.of(PassSecretValueSource::class) {
                this.parameters.passName.set("solidblocks/aws/test/access_key_id")
            }.get(),
            "AWS_SECRET_ACCESS_KEY" to providers.of(PassSecretValueSource::class) {
                this.parameters.passName.set("solidblocks/aws/test/secret_access_key")
            }.get(),
        )
    )
}
