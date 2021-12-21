plugins {
    id("solidblocks.kotlin-library-conventions")
    id("solidblocks.kotlin-application-conventions")
    id("com.palantir.docker") version "0.31.0"
}

dependencies {

    implementation(project(":solidblocks-vault"))
    implementation(project(":solidblocks-base"))

    implementation("com.github.ajalt.clikt:clikt:3.1.0")
    implementation("com.github.docker-java:docker-java-core:3.2.12")
    implementation("com.github.docker-java:docker-java-transport-zerodep:3.2.12")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("com.michael-bull.kotlin-retry:kotlin-retry:1.0.9")
    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.7.1")

    implementation("org.springframework.vault:spring-vault-core:2.3.2")

    testImplementation(project(":solidblocks-core"))
    testImplementation(project(":solidblocks-provisioner"))
    testImplementation(project(":solidblocks-cloud"))
    testImplementation(project(":solidblocks-cloud-config"))
    testImplementation(project(":solidblocks-test"))
}

docker {
    setDockerfile(file("$projectDir/docker/Dockerfile"))
    files(file("$projectDir/docker/install.sh"))
    name = "solidblocks-service-vault"
}

application {
    mainClass.set("de.solidblocks.service.vault.CliKt")
}
