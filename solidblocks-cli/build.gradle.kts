plugins {
    kotlin("multiplatform") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
}

group = "de.solidblocks.cli"
version = "0.0.1"


repositories {
    mavenCentral()
}

kotlin {
    linuxArm64 {
        binaries { executable() }
    }
    linuxX64 {
        binaries { executable() }
    }
    macosX64 {
        binaries { executable() }
    }
    macosArm64 {
        binaries { executable() }
    }
    mingwX64 {
        binaries { executable() }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("com.charleskorn.kaml:kaml:0.61.0")
                implementation("com.squareup.okio:okio:3.9.0")
                implementation("com.saveourtool.okio-extras:okio-extras:1.1.3")
                implementation("com.github.ajalt.clikt:clikt:4.4.0")
                implementation("com.kgit2:kommand:2.2.1")
                implementation("io.ktor:ktor-client-resources:3.0.0-beta-2")
                implementation("io.ktor:ktor-client-content-negotiation:3.0.0-beta-2")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0-beta-2")
            }
        }
        linuxMain {
            dependencies {
                implementation("io.ktor:ktor-client-curl:3.0.0-beta-2")
            }
        }
        appleMain {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:3.0.0-beta-2")
            }
        }
        mingwMain {
            dependencies {
                implementation("io.ktor:ktor-client-winhttp:3.0.0-beta-2")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("com.willowtreeapps.assertk:assertk:0.28.1")
                implementation("com.squareup.okio:okio-fakefilesystem:3.9.0")

            }
        }
    }
}

fun pass(path: String): String {
    val pb: ProcessBuilder = ProcessBuilder("pass", path)
    val process = pb.start()
    return String(process.inputStream.readAllBytes()).trim()
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest>().configureEach {
    environment("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN") ?: pass("solidblocks/hetzner/test/hcloud_api_token"))
}
