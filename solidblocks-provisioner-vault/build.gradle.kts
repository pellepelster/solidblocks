plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-cloud-config"))

    // TODO remove last spring dependency
    api("org.springframework.vault:spring-vault-core:2.3.2")

    testImplementation("org.testcontainers:testcontainers:1.15.3")
    testImplementation("org.mockito:mockito-core:3.+")
    testImplementation("org.apache.derby:derbytools:10.15.2.0")
    testImplementation("org.apache.derby:derby:10.15.2.0")
}
