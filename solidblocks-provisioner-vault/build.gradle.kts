import Constants.SPRING_BOOT_VERSION

plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-cloud-config"))
    api("org.springframework.vault:spring-vault-core:2.3.2")

    testImplementation("org.springframework:spring-test:5.3.6")
    testImplementation("org.springframework.boot:spring-boot-test:${SPRING_BOOT_VERSION}")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure:${SPRING_BOOT_VERSION}")
    testImplementation("org.testcontainers:testcontainers:1.15.3")
    testImplementation("org.mockito:mockito-core:3.+")
    testImplementation("com.h2database:h2:1.4.200")
}
