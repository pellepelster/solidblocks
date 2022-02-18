plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {

    implementation(project(":solidblocks-provisioner-minio"))
    implementation(project(":solidblocks-service-vault-api"))
    implementation(project(":solidblocks-provisioner-vault"))
    implementation(project(":solidblocks-service-vault"))
}
