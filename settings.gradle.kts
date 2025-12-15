pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "solidblocks"

include(
    "solidblocks-test",
    "solidblocks-shell",
    "solidblocks-hetzner-cloud",
    "solidblocks-rds-postgresql-docker",
    "solidblocks-debug-container",
    "solidblocks-cli",
    "solidblocks-hetzner-web-s3-docker"
)