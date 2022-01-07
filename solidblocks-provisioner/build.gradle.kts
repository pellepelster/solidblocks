plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-api"))
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-cloud-init"))
    implementation(project(":solidblocks-provisioner-hetzner"))

    implementation("io.github.resilience4j:resilience4j-kotlin:1.3.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.3.1")
}
