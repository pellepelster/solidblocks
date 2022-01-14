plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {

    implementation("org.springframework.vault:spring-vault-core:2.3.2")

    implementation(project(":solidblocks-base"))

    implementation("io.github.hakky54:sslcontext-kickstart:7.2.0")
    implementation("io.github.hakky54:sslcontext-kickstart-for-pem:7.2.0")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    testImplementation(project(":solidblocks-test"))
}
