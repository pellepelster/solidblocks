plugins {
    id("solidblocks.kotlin-application-conventions")
}

dependencies {
    implementation(project(":solidblocks-provisioner-hetzner"))
    implementation(project(":solidblocks-cloud-config"))
    implementation(project(":solidblocks-cloud-init"))
    implementation(project(":solidblocks-provisioner"))
    implementation(project(":solidblocks-provisioner-vault"))
    implementation(project(":solidblocks-base"))

    implementation("org.springframework.boot:spring-boot-starter:2.4.5")

    implementation("org.apache.derby:derby:10.15.2.0")
    implementation("org.apache.derby:derbytools:10.15.2.0")

    implementation("com.hierynomus:sshj:0.31.0")
    implementation("org.apache.commons:commons-text")
    implementation("com.github.ajalt.clikt:clikt:3.1.0")

    testImplementation("org.springframework.boot:spring-boot-test:2.4.5")
    testImplementation("org.springframework:spring-test:5.3.6")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.testcontainers:testcontainers:1.15.3")
}

application {
    mainClass.set("de.solidblocks.cli.CliKt")
}
