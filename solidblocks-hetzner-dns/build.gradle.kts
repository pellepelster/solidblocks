import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("com.vanniktech.maven.publish") version "0.29.0"
}

group = "de.solidblocks"
version = System.getenv("VERSION") ?: "0.0.0"

mavenPublishing {
    coordinates("de.solidblocks", "hetzner-dns", "${version}")

    pom {

        name.set("Solidblocks DNS")
        description.set("Solidblocks is a library of reusable components for infrastructure operation, automation and developer experience")
        inceptionYear.set("2019")
        url.set("https://github.com/pellepelster/solidblocks/")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("https://opensource.org/license/mit")
            }
        }

        developers {
            developer {
                id.set("pellepelster")
                name.set("Christian 'Pelle' Pelster")
                url.set("https://github.com/pellepelster/")
            }
        }

        scm {
            url.set("https://github.com/pellepelster/solidblocks/")
            connection.set("scm:git:git://github.com/pellepelster/solidblocks.git")
            developerConnection.set("scm:git:ssh://git@github.com/pellepelster/solidblocks.git")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    testImplementation("org.slf4j:slf4j-simple:2.0.13")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation("org.wiremock:wiremock:3.9.1")
}


tasks.withType<Test> {
    useJUnitPlatform()

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
            "HETZNER_DNS_API_TOKEN" to providers.of(PassSecretValueSource::class) {
                this.parameters.passName.set("solidblocks/hetzner/test/dns_api_token")
            }.get(),
        )
    )
}