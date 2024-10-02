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
                implementation("it.krzeminski:snakeyaml-engine-kmp:3.0.2")
                implementation("com.kgit2:kommand:2.2.1")
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
