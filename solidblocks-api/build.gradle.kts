plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    api(project(":solidblocks-core"))
    implementation("org.jgrapht:jgrapht-core:1.5.1")
}
