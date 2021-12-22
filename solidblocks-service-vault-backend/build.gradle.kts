plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {

    implementation(project(":solidblocks-cloud"))
    implementation(project(":solidblocks-provisioner-minio"))
    implementation(project(":solidblocks-service-vault-api"))
}
