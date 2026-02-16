plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
}

dependencies {
    implementation(project(":solidblocks-utils"))
    implementation(project(":solidblocks-ssh"))
    implementation(project(":solidblocks-hetzner-cloud"))
    implementation(project(":solidblocks-cloud-init"))
    implementation(project(":solidblocks-fr-deuxfleurs-garagehq"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jgrapht:jgrapht-core:1.5.1")
    implementation("io.github.resilience4j:resilience4j-retry:2.1.0")
    implementation("com.charleskorn.kaml:kaml:0.83.0")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
}
