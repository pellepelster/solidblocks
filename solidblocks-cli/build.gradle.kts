plugins {
    id("solidblocks.kotlin-application-conventions")
}

dependencies {
    implementation(project(":solidblocks-cloud"))
    implementation(project(":solidblocks-provisioner-vault"))
    implementation(project(":solidblocks-provisioner-consul"))
    implementation(project(":solidblocks-provisioner-hetzner"))

    api("org.apache.derby:derby:10.15.2.0")
    api("org.apache.derby:derbytools:10.15.2.0")

    implementation("com.hierynomus:sshj:0.31.0")
    implementation("org.apache.commons:commons-text")
    implementation("com.github.ajalt.clikt:clikt:3.1.0")

    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.testcontainers:testcontainers:1.15.3")
}

application {
    mainClass.set("de.solidblocks.cli.CliKt")
}
