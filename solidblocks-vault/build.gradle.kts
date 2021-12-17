plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {

    implementation("org.springframework.vault:spring-vault-core:2.3.2")
    implementation(project(":solidblocks-cloud-config"))
    implementation(project(":solidblocks-base"))

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    testImplementation("org.awaitility:awaitility:4.1.1")
    testImplementation("org.awaitility:awaitility-kotlin:4.1.1")

    testImplementation(project(":solidblocks-test"))
}
