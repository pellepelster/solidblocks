import Constants.dockerRepository

plugins {
    id("solidblocks.kotlin-application-conventions")
    id("solidblocks.kotlin-publish-conventions")
    id("com.palantir.docker") version "0.32.0"
}

dependencies {
    implementation(project(":solidblocks-agent-base"))

    testImplementation("com.squareup.okhttp3:okhttp:4.9.3")
    testImplementation(project(":solidblocks-test"))

    testImplementation(project(":solidblocks-cloud"))
    testImplementation("com.github.docker-java:docker-java-core:3.2.12")
    testImplementation("com.github.docker-java:docker-java-transport-zerodep:3.2.12")
}

application {
    mainClass.set("de.solidblocks.service.helloworld.agent.CliKt")
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            artifact(tasks.distTar)
        }
    }
}

docker {
    setDockerfile(file("$projectDir/docker/Dockerfile"))
    name = "$dockerRepository/solidblocks-helloworld:$version"
}