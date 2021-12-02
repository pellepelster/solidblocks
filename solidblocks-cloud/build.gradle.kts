plugins {
    id("solidblocks.kotlin-application-conventions")
}

dependencies {
    implementation(project(":solidblocks-provisioner-hetzner"))
    implementation(project(":solidblocks-cloud-config"))
    implementation(project(":solidblocks-cloud-init"))
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-provisioner-vault"))
}

application {
    mainClass.set("de.solidblocks.cli.CliKt")
}
