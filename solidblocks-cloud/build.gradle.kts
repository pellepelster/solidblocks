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

    implementation("org.bouncycastle:bcprov-jdk15to18:1.70")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc:4.32.0")
    implementation("com.github.kagkarlsson:db-scheduler:10.5")

    implementation("io.vertx:vertx-web:4.2.3")
    implementation("io.vertx:vertx-lang-kotlin:4.2.3")
    implementation("io.vertx:vertx-auth-jwt:4.2.3")
    implementation("io.vertx:vertx-ext:38")

    implementation("com.github.seancfoley:ipaddress:5.3.3")

    testImplementation("io.rest-assured:rest-assured:4.4.0")
    testImplementation(project(":solidblocks-test"))
}

application {
    mainClass.set("de.solidblocks.cli.CliKt")
}
