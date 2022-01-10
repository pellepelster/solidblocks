plugins {
    id("solidblocks.kotlin-application-conventions")
}

dependencies {
    implementation(project(":solidblocks-cloud"))
    implementation(project(":solidblocks-agent-base"))
    implementation(project(":solidblocks-provisioner-vault"))
    implementation(project(":solidblocks-provisioner-consul"))
    implementation(project(":solidblocks-provisioner-hetzner"))
    implementation(project(":solidblocks-agent-base-api"))

    api("org.apache.derby:derby:10.15.2.0")
    api("org.apache.derby:derbytools:10.15.2.0")

    implementation("com.hierynomus:sshj:0.31.0")
    implementation("org.apache.commons:commons-text")

    testImplementation("org.hamcrest:hamcrest:2.2")
}

application {
    mainClass.set("de.solidblocks.cli.CliKt")
}
