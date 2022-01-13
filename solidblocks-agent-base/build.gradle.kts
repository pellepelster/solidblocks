plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {

    api(project(":solidblocks-base"))
    implementation(project(":solidblocks-cloud"))
    implementation(project(":solidblocks-cloud-model"))
    implementation(project(":solidblocks-agent-base-api"))
    implementation(project(":solidblocks-vault"))

    implementation("io.vertx:vertx-web:4.2.3")
    implementation("io.vertx:vertx-lang-kotlin:4.2.3")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("com.github.docker-java:docker-java-core:3.2.12")
    implementation("com.github.docker-java:docker-java-transport-zerodep:3.2.12")

    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.7.1")

    testImplementation(project(":solidblocks-test"))
}
