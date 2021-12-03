plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    implementation(project(":solidblocks-cloud-config"))
    implementation(project(":solidblocks-base"))
    implementation(project(":solidblocks-provisioner"))

    api("me.tomsdevsn:hetznercloud-api:2.13.0")
    implementation("org.springframework:spring-context:5.3.6")
    implementation("org.springframework:spring-web:5.3.6")
    implementation("io.pelle.hetzner:hetznerdns-java:0.1.0")
}
