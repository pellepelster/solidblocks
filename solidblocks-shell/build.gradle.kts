plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
}

dependencies {
    testImplementation(project(":solidblocks-test"))
    testImplementation("org.wiremock:wiremock:3.9.1")
}