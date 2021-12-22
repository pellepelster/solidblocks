plugins {
    id("solidblocks.kotlin-library-conventions")
    id("solidblocks.kotlin-application-conventions")
    id("com.palantir.docker") version "0.31.0"
}

val ktorVersion = "1.5.4"

dependencies {

    implementation(project(":solidblocks-vault"))
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-cloud-model"))
    implementation(project(":solidblocks-service-base"))

    implementation("org.slf4j:slf4j-jdk14:2.0.0-alpha5")

    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.dropwizard.metrics:metrics-core:4.1.18")
    implementation("io.ktor:ktor-metrics:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    implementation(
        project(
            mapOf(
                "path" to ":solidblocks-ingress-api",
                "configuration" to "server"
            )
        )
    )

    implementation("com.github.ajalt.clikt:clikt:3.1.0")

    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.7.1")

    testImplementation(project(":solidblocks-cloud-model"))
    testImplementation(project(":solidblocks-test"))
}

docker {
    setDockerfile(file("$projectDir/docker/Dockerfile"))
    name = "solidblocks-ingress"
}

application {
    mainClass.set("de.solidblocks.ingress.CliKt")
}
