plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("buildlogic.solidblocks-releasetest-conventions")
}

dependencies {
    testImplementation(project(":solidblocks-test"))
    testImplementation("org.wiremock:wiremock:3.9.1")
    releaseTestImplementation(project(":solidblocks-test"))
}