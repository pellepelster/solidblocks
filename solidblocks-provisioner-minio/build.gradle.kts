plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-cloud-config"))

    implementation("io.minio:minio:8.3.4")

    testImplementation("org.testcontainers:testcontainers:1.15.3")
    testImplementation("org.apache.derby:derbytools:10.15.2.0")
    testImplementation("org.apache.derby:derby:10.15.2.0")
}
