plugins {
    id("solidblocks.kotlin-application-conventions")
}

dependencies {
    implementation(project(":solidblocks-provisioner-hetzner"))
    implementation(project(":solidblocks-cloud-config"))
    implementation(project(":solidblocks-cloud-init"))
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-api"))
    implementation(project(":solidblocks-core"))
    implementation(project(":solidblocks-vault"))
    implementation(project(":solidblocks-provisioner-vault"))
    implementation(project(":solidblocks-provisioner-consul"))
    implementation(project(":solidblocks-provisioner-minio"))
    implementation("com.github.seancfoley:ipaddress:5.3.3")
}

application {
    mainClass.set("de.solidblocks.cli.CliKt")
}
