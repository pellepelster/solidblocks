plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-core"))
    implementation("org.jgrapht:jgrapht-core:1.5.1")
}
