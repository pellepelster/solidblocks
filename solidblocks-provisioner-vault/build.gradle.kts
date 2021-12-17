plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-api"))
    implementation(project(":solidblocks-core"))
    implementation(project(":solidblocks-cloud-config"))

    // TODO remove last spring dependency
    api("org.springframework.vault:spring-vault-core:2.3.2")
    implementation(project(":solidblocks-vault"))

    testImplementation("org.testcontainers:testcontainers:1.15.3")
    testImplementation(project(":solidblocks-test"))
}
