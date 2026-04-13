import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("org.graalvm.buildtools.native") version "0.11.0"
    application
}

dependencies {
    implementation(project(":solidblocks-hetzner-cloud"))
    implementation(project(":solidblocks-utils"))
    implementation(project(":solidblocks-cloud"))
    implementation(project(":solidblocks-ssh"))

    implementation("io.ktor:ktor-client-content-negotiation:3.1.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.2")
    implementation("io.ktor:ktor-client-java:3.1.2")
    implementation("io.ktor:ktor-client-logging:3.1.2")

    implementation("org.graalvm.sdk:nativeimage:23.1.7")

    implementation("com.github.ajalt.clikt:clikt-markdown:5.0.3")
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("com.charleskorn.kaml:kaml:0.83.0")

    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.77")
    implementation("org.bouncycastle:bcprov-ext-jdk18on:1.77")

    implementation("aws.sdk.kotlin:s3-jvm:1.5.26")
    implementation("aws.sdk.kotlin:s3:1.5.26")
    implementation("aws.sdk.kotlin:iam-jvm:1.5.26")
    implementation("aws.sdk.kotlin:iam:1.5.26")

    implementation("org.slf4j:slf4j-jdk14:2.0.17")

    testImplementation(project(":solidblocks-test"))
    testImplementation("org.reflections:reflections:0.10.2")
    testImplementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
    testImplementation("com.jayway.jsonpath:json-path:2.9.0")
}

val integrationTest = sourceSets.create("integrationTest") {
    compileClasspath += sourceSets["test"].compileClasspath
    runtimeClasspath += sourceSets["test"].runtimeClasspath
}

tasks.register<Test>("testIntegration") {
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath

    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    environment(
        mapOf(
            "HCLOUD_TOKEN" to providers.of(PassSecretValueSource::class) {
                this.parameters.path.set("solidblocks/hetzner/test/hcloud_api_token")
                this.parameters.environment.set("HCLOUD_TOKEN")
            }.get(),
        )
    )

}

val generateTask = tasks.register<Test>("generate") {
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform {
        includeTags("generate")
    }

    outputs.upToDateWhen { false }

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.getAt("generateResourcesConfigFile").dependsOn(generateTask)

graalvmNative {
    agent {
    }

    binaries {
        named("main") {
            imageName.set("blcks")
            buildArgs(
                listOf(
                    "-Os",
                    "--no-fallback",
                    "--features=de.solidblocks.cli.BouncyCastleFeature",
                    "--initialize-at-build-time",
                    "--initialize-at-run-time=org.slf4j.LoggerFactory",
                    "--initialize-at-run-time=org.bouncycastle.crypto.prng.SP800SecureRandom",
                    "--initialize-at-run-time=org.bouncycastle.jcajce.provider.drbg.DRBG\$NonceAndIV",
                    "--initialize-at-run-time=org.bouncycastle.jcajce.provider.drbg.DRBG\$Default",
                    "--initialize-at-run-time=org.apache.sshd.common.random.JceRandom\$Cache",
                    "--initialize-at-run-time=org.xbill.DNS.Header",
                    "--initialize-at-run-time=org.xbill.DNS.Lookup",
                    "--initialize-at-run-time=org.xbill.DNS.io.DefaultIoClientFactory",
                    "-H:IncludeResources=logging.properties",
                    "-H:-AddAllFileSystemProviders",
                    "-H:+UnlockExperimentalVMOptions",
                    "-H:-ReduceImplicitExceptionStackTraceInformation",
                    "-H:ReflectionConfigurationFiles=${projectDir}/src/main/resources/META-INF/native-image/reflect-config-generated.json"
                )
            )
        }
    }
}

application {
    mainClass = "de.solidblocks.cli.MainKt"
    applicationName = "blcks"
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "24"
    targetCompatibility = "24"
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("24")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}
