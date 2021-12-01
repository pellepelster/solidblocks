import Constants.SPRING_BOOT_VERSION

plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-cloud-config"))
    implementation("com.orbitz.consul:consul-client:1.5.3")

    testImplementation("org.assertj:assertj-core:3.21.0")
    testImplementation("org.springframework:spring-test:5.3.6")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure:${SPRING_BOOT_VERSION}")
    testImplementation("org.springframework.boot:spring-boot-test:${SPRING_BOOT_VERSION}")
    testImplementation("org.testcontainers:testcontainers:1.15.3")
    testImplementation("com.ninja-squad:springmockk:3.0.1")
}
