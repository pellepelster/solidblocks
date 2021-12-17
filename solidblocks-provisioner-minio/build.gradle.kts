plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-api"))
    implementation(project(":solidblocks-core"))
    implementation(project(":solidblocks-cloud-config"))

    implementation("io.minio:minio:8.3.4")

    testImplementation("org.testcontainers:testcontainers:1.15.3")
}
