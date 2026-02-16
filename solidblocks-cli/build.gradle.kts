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

    implementation("com.github.ajalt.clikt:clikt-markdown:5.0.3")
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("com.charleskorn.kaml:kaml:0.83.0")

    implementation("aws.sdk.kotlin:s3-jvm:1.5.26")
    implementation("aws.sdk.kotlin:s3:1.5.26")

    /*
    implementation("ch.qos.logback:logback-core:1.5.18")
    implementation("ch.qos.logback:logback-classic:1.5.18")
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
    binaries {
        named("main") {
            imageName.set("blcks")
            buildArgs(
                listOf(
                    "-Os",
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