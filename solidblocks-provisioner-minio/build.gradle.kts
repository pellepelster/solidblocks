plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    api(project(":solidblocks-provisioner"))
    api(project(":solidblocks-cloud-model"))

    implementation("io.minio:minio:8.3.4")
}
