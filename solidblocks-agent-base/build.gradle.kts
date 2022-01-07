import Constants.ktorVersion

plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {

    api(project(":solidblocks-base"))
    implementation(project(":solidblocks-cloud-model"))

    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.dropwizard.metrics:metrics-core:4.1.18")
    implementation("io.ktor:ktor-metrics:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("com.github.docker-java:docker-java-core:3.2.12")
    implementation("com.github.docker-java:docker-java-transport-zerodep:3.2.12")

    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.7.1")

    testImplementation("org.slf4j:slf4j-jdk14:1.7.30")
    testImplementation(project(":solidblocks-test"))
}
