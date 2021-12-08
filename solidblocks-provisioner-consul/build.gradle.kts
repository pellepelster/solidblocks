plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-cloud-config"))
    api("com.orbitz.consul:consul-client:1.5.3")

    testImplementation("org.testcontainers:testcontainers:1.15.3")
}
