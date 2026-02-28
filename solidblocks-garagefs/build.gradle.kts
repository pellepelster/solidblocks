plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
}

dependencies {
    implementation("org.slf4j:slf4j-jdk14:2.0.17")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.0")
    implementation("io.ktor:ktor-client-resources:3.1.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.2")
    implementation("io.ktor:ktor-client-java:3.1.2")
}
