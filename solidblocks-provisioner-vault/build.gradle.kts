plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-cloud-config"))
    api("org.springframework.vault:spring-vault-core:2.3.2")

    testImplementation("org.springframework:spring-test:5.3.6")
    testImplementation("org.springframework.boot:spring-boot-test:2.4.5")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure:2.4.5")
    testImplementation("org.testcontainers:testcontainers:1.15.3")
    testImplementation("com.h2database:h2:1.4.200")
}
