plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-cloud"))
    implementation(project(":solidblocks-api"))
    implementation(project(":solidblocks-cloud-config"))
    implementation(project(":solidblocks-vault"))
    implementation(project(":solidblocks-provisioner-vault"))
    implementation(project(":solidblocks-provisioner-minio"))
    implementation("org.junit.jupiter:junit-jupiter-api:5.7.1")

    implementation("org.testcontainers:testcontainers:1.15.3")
}
