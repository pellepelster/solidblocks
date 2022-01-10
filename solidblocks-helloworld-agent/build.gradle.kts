plugins {
    id("solidblocks.kotlin-application-conventions")
    id("solidblocks.kotlin-publish-conventions")
}

dependencies {
    implementation(project(":solidblocks-agent-base"))
    implementation(project(":solidblocks-agent-base-api"))

    testImplementation("com.squareup.okhttp3:okhttp:4.9.3")
    testImplementation(project(":solidblocks-test"))
}

application {
    mainClass.set("de.solidblocks.helloworld.agent.CliKt")
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            artifact(tasks.distTar)
        }
    }
}
