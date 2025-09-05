plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("org.graalvm.buildtools.native") version "0.11.0"
    id("com.gradleup.shadow") version "9.1.0"
    application
}

group = "de.solidblocks"
version = System.getenv("VERSION") ?: "0.0.0"

dependencies {
    implementation("io.ktor:ktor-client-resources:3.1.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.2")
    implementation("io.ktor:ktor-client-java:3.1.2")
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("com.kgit2:kommand:2.2.1")
    implementation("com.charleskorn.kaml:kaml:0.61.0")
    implementation("com.squareup.okio:okio:3.9.0")
    implementation("com.saveourtool.okio-extras:okio-extras:1.1.3")
    implementation("io.ktor:ktor-client-resources:3.1.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.2")

    implementation("aws.sdk.kotlin:s3-jvm:1.5.26")
    implementation("aws.sdk.kotlin:s3:1.5.26")

    testImplementation(kotlin("test"))
    testImplementation("org.reflections:reflections:0.10.2")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("blcks")
            buildArgs(
                listOf(
                    "--no-fallback",
                    "--initialize-at-build-time",
                    "-H:+UnlockExperimentalVMOptions",
                    "-H:ReflectionConfigurationFiles=${projectDir}/src/main/resources/META-INF/native-image/reflect-config.json"
                )
            )
        }
    }
}

application {
    mainClass = "de.solidblocks.cli.MainKt"
    applicationName = "blcks"
}