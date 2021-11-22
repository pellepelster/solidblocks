plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    api(project(":solidblocks-core"))

    implementation("io.github.resilience4j:resilience4j-retry:1.7.0")
    implementation("com.jcabi:jcabi-manifests:1.1")

    implementation("org.bouncycastle:bcprov-jdk15to18:1.68")
    implementation("net.i2p.crypto:eddsa:0.3.0")

    testImplementation("org.hamcrest:hamcrest:2.2")
}