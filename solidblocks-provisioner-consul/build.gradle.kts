plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-cloud-model"))
    implementation(project(":solidblocks-core"))

    api("com.orbitz.consul:consul-client:1.5.3")
}
