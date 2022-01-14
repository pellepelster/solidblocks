plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-cloud-model"))

    implementation("joda-time:joda-time:2.10.10")

    // TODO remove last spring dependency
    api("org.springframework.vault:spring-vault-core:2.3.2")
    api(project(":solidblocks-vault"))

    testImplementation(project(":solidblocks-test"))
}
