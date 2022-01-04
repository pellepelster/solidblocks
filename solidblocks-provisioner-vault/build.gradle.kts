plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-cloud-model"))

    // TODO remove last spring dependency
    api("org.springframework.vault:spring-vault-core:2.3.2")
    api(project(":solidblocks-vault"))

    testImplementation(project(":solidblocks-test"))
}
