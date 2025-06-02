pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "solidblocks"

include("solidblocks-test", "solidblocks-shell", "solidblocks-hetzner-dns", "solidblocks-rds-postgresql-docker", "solidblocks-terraform", "solidblocks-debug-container")