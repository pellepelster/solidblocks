import Constants.testContainersVersion

plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-cloud"))
    implementation(project(":solidblocks-provisioner-vault"))
    implementation(project(":solidblocks-service-vault-api"))
    implementation(project(":solidblocks-service-vault-backend"))
    implementation(project(":solidblocks-provisioner-minio"))
    implementation("org.junit.jupiter:junit-jupiter-api:5.7.1")

    implementation("org.testcontainers:testcontainers:$testContainersVersion")
}
