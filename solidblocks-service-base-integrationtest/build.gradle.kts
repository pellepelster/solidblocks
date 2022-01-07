

plugins {
    id("solidblocks.kotlin-application-conventions")
}

dependencies {

    implementation(project(":solidblocks-service-base"))
    implementation(project(":solidblocks-core"))

    implementation("io.ktor:ktor-client-core:1.6.7")
    implementation("io.ktor:ktor-client-cio:1.6.7")
    implementation("io.ktor:ktor-client-serialization:1.6.7")
    implementation("io.ktor:ktor-client-jackson:1.6.7")

    testImplementation("com.squareup.okhttp3:okhttp:4.9.3")
    testImplementation("org.slf4j:slf4j-jdk14:1.7.30")
    testImplementation(project(":solidblocks-test"))
}

application {
    mainClass.set("de.solidblocks.service.integrationtest.CliKt")
}
