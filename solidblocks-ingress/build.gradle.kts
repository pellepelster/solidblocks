import Constants.ktorVersion

plugins {
    id("solidblocks.kotlin-application-conventions")
    id("com.palantir.docker") version "0.31.0"
}


dependencies {

    implementation(project(":solidblocks-vault"))
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-cloud-model"))
    implementation(project(":solidblocks-service-base"))

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

    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.7.1")

    testImplementation(project(":solidblocks-cloud-model"))
    testImplementation(project(":solidblocks-test"))

    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.28.0")
}

docker {
    setDockerfile(file("$projectDir/docker/Dockerfile"))
    name = "solidblocks-ingress"
}
tasks.getByPath("check").dependsOn(tasks.getByPath("docker"))

application {
    mainClass.set("de.solidblocks.ingress.CliKt")
}
