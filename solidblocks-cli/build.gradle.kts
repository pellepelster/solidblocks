plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("org.graalvm.buildtools.native") version "0.11.0"
    application
}

dependencies {
    implementation(project(":solidblocks-hetzner-cloud"))
    implementation(project(":solidblocks-utils"))
    implementation(project(":solidblocks-cloud"))

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

    implementation(project(":solidblocks-ssh"))

    implementation("org.slf4j:slf4j-jdk14:2.0.17")

    /*
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("ch.qos.logback:logback-core:1.5.18")
    */

    testImplementation("org.reflections:reflections:0.10.2")
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
        enabled.set(true)
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
                    "--initialize-at-build-time=org.bouncycastle.crypto.prng.SP800SecureRandom",
                    "--initialize-at-run-time=org.bouncycastle.jcajce.provider.drbg.DRBG\$NonceAndIV",
                    "--initialize-at-run-time=org.bouncycastle.jcajce.provider.drbg.DRBG\$Default",
                    "--initialize-at-run-time=org.apache.sshd.common.random.JceRandom\$Cache",
                    "--initialize-at-run-time=de.solidblocks.cli.BCInitializer",
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