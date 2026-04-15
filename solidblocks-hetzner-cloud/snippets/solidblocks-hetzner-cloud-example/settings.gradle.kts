plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "solidblocks-hetzner-cloud-example"

/*
includeBuild("../../../") {
  dependencySubstitution {
    substitute(module("de.solidblocks:hetzner-cloud")).using(project(":solidblocks-hetzner-cloud"))
  }
}
*/