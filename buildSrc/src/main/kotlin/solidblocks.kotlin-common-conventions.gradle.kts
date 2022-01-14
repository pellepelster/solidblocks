import Constants.junitJupiterVersion
import Constants.testContainersVersion
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("net.nemerosa.versioning")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
}

versioning {
}

val versionFile = File("$rootDir/version.txt")

version = System.getenv("SOLIDBLOCKS_VERSION") ?: versionFile.readText().trim()


dependencies {
    constraints {
        implementation("org.apache.commons:commons-text:1.9")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("ch.qos.logback:logback-classic:1.2.1")

    implementation("io.github.microutils:kotlin-logging-jvm:2.0.6")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")

    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")

    testImplementation("org.assertj:assertj-core:3.21.0")
    testImplementation("org.apache.derby:derbytools:10.15.2.0")
    testImplementation("org.apache.derby:derby:10.15.2.0")

    testImplementation("org.awaitility:awaitility:4.1.1")
    testImplementation("org.awaitility:awaitility-kotlin:4.1.1")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        suppressWarnings = true
        jvmTarget = "16"
        allWarningsAsErrors = false
        noJdk = false
    }
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events = setOf(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR
        )
    }
}


tasks.jar {
    manifest {
        attributes["Commit"] = versioning.info.commit
        attributes["Solidblocks-Version"] = version
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    outputToConsole.set(true)
    outputColorName.set("RED")
    disabledRules.set(listOf("no-wildcard-imports", "redundant-curly-braces"))
    ignoreFailures.set(true)

    reporters {
        reporter(ReporterType.PLAIN_GROUP_BY_FILE)
    }
    filter {
        exclude("**/build/**")
    }
}
