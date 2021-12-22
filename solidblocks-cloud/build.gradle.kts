plugins {
    id("solidblocks.kotlin-application-conventions")
}

dependencies {
    api(project(":solidblocks-cloud-model"))
    api(project(":solidblocks-provisioner"))
    api(project(":solidblocks-provisioner-hetzner"))
    api(project(":solidblocks-provisioner-vault"))
    api(project(":solidblocks-provisioner-consul"))
    api(project(":solidblocks-provisioner-minio"))
    implementation("com.github.seancfoley:ipaddress:5.3.3")
}

application {
    mainClass.set("de.solidblocks.cli.CliKt")
}
