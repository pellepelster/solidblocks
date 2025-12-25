plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "solidblocks-test-gradle"

includeBuild("../../../") {
    dependencySubstitution {
        substitute(module("de.solidblocks:infra-test")).using(project(":solidblocks-test"))
    }
}
