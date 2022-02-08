plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    api(project(":solidblocks-core"))
    api(project(":solidblocks-api"))

    implementation("io.github.resilience4j:resilience4j-retry:1.7.0")
    implementation("com.jcabi:jcabi-manifests:1.1")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("dnsjava:dnsjava:3.5.0")

    testImplementation("org.hamcrest:hamcrest:2.2")
}
