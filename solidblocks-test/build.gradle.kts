import java.net.URI

plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("maven-publish")
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = URI.create("https://maven.pkg.github.com/pellepelster/solidblocks")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = "de.solidblocks"
            artifactId = "infra-test"
            version = System.getenv("VERSION") ?: "snapshot"
            from(components["java"])
        }
    }
}

object Versions {
    const val testContainersVersion = "1.17.1"
}

dependencies {
    api("io.kotest:kotest-assertions-core:5.9.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.slf4j:slf4j-simple:2.0.13")
    implementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    implementation("com.github.docker-java:docker-java:3.4.0")
    implementation("com.github.docker-java:docker-java-transport-zerodep:3.4.0")

}
