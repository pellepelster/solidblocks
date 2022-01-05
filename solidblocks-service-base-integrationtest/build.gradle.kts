import Constants.ktorVersion

plugins {
    id("solidblocks.kotlin-application-conventions")
}

dependencies {

    implementation(project(":solidblocks-service-base"))

    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.dropwizard.metrics:metrics-core:4.1.18")
    implementation("io.ktor:ktor-metrics:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    testImplementation("org.slf4j:slf4j-jdk14:1.7.30")
    testImplementation(project(":solidblocks-test"))
}

application {
    mainClass.set("de.solidblocks.service.integrationtest.CliKt")
}
