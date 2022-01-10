plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    api(project(":solidblocks-base"))
    testImplementation(project(":solidblocks-test"))
}
