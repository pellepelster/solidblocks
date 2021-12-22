plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-cloud-model"))

    implementation("io.minio:minio:8.3.4")

    testImplementation("org.testcontainers:testcontainers:1.15.3")
}
