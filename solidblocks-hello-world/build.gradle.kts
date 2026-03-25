plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    application
}

repositories {
    mavenCentral()
}

group = "de.solidblocks"
version = "0.0.1"

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:2.3.13")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.13")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.13")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.13")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

    implementation("ch.qos.logback:logback-classic:1.5.18")

    implementation("org.jetbrains.exposed:exposed-core:0.55.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("com.zaxxer:HikariCP:5.1.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

application {
    mainClass = "de.solidblocks.helloworld.ApplicationKt"
}
