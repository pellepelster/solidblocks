plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    api(project(":solidblocks-api"))
    api(project(":solidblocks-base"))
    api(project(":solidblocks-provisioner-hetzner"))

    implementation("io.github.resilience4j:resilience4j-kotlin:1.3.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.3.1")

    testImplementation("junit:junit:4.13.2")
}
