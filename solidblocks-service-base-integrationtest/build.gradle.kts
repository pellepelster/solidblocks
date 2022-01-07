

plugins {
    id("solidblocks.kotlin-application-conventions")
}

dependencies {

    implementation(project(":solidblocks-service-base"))
    implementation(project(":solidblocks-base"))

    testImplementation("com.squareup.okhttp3:okhttp:4.9.3")

    testImplementation("org.slf4j:slf4j-jdk14:1.7.30")
    testImplementation(project(":solidblocks-test"))
}

application {
    mainClass.set("de.solidblocks.service.integrationtest.CliKt")
}
