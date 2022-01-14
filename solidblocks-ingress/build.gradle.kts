import Constants.dockerRepository

plugins {
    id("solidblocks.kotlin-application-conventions")
    id("solidblocks.kotlin-publish-conventions")
    id("com.palantir.docker") version "0.32.0"
}

dependencies {

    implementation(project(":solidblocks-vault"))
    implementation(project(":solidblocks-cloud-model"))
    implementation(project(":solidblocks-cloud"))
    implementation(project(":solidblocks-agent-base"))
    implementation(project(":solidblocks-ingress-api"))

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.7.1")

    testImplementation(project(":solidblocks-cloud-model"))
    testImplementation(project(":solidblocks-test"))

    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.28.0")
}

docker {
    setDockerfile(file("$projectDir/docker/Dockerfile"))
    name = "$dockerRepository/solidblocks-ingress:$version"
}
tasks.getByPath("check").dependsOn(tasks.getByPath("docker"))

application {
    mainClass.set("de.solidblocks.ingress.CliKt")
}
