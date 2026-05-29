import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.testcontainers:testcontainers:2.0.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation("io.github.microutils:kotlin-logging-jvm:2.0.6")
    testImplementation("org.assertj:assertj-core:3.27.7")

    testImplementation("com.github.docker-java:docker-java:3.5.1")
    testImplementation("com.github.docker-java:docker-java-transport-zerodep:3.5.1")

    testImplementation("org.glassfish:javax.json:1.1.2")

    testImplementation("io.minio:minio:8.6.0")

    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")

    testImplementation("ch.qos.logback:logback-classic:1.5.13")
    testImplementation("org.postgresql:postgresql:42.7.2")
    testImplementation("org.jdbi:jdbi3-core:3.32.0")

    testImplementation("com.google.cloud:google-cloud-storage:2.34.0")

    testImplementation(project(":solidblocks-test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperties["junit.jupiter.execution.parallel.enabled"] = "true"
    systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
    systemProperties["junit.jupiter.execution.parallel.config.strategy"] = "fixed"
    systemProperties["junit.jupiter.execution.parallel.config.fixed.parallelism"] = 5
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
                this.parameters.path = "solidblocks/hetzner/test/s3_access_key_id"
            }.get(),
            "HETZNER_S3_SECRET_KEY" to providers.of(PassSecretValueSource::class) {
                this.parameters.path = "solidblocks/hetzner/test/s3_secret_key"
            }.get(),
            "AWS_ACCESS_KEY_ID" to providers.of(PassSecretValueSource::class) {
                this.parameters.path = "solidblocks/aws/test/access_key_id"
            }.get(),
            "AWS_SECRET_ACCESS_KEY" to providers.of(PassSecretValueSource::class) {
                this.parameters.path = "solidblocks/aws/test/secret_access_key"
            }.get(),
        )
    )
}
