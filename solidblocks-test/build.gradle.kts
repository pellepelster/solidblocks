import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("com.vanniktech.maven.publish") version "0.29.0"
}

group = "de.solidblocks"
version = System.getenv("VERSION") ?: "0.0.0"

mavenPublishing {
    coordinates("de.solidblocks", "infra-test", "${version}")

    pom {

        name.set("Solidblocks Test")
        description.set("Solidblocks is a library of reusable components for infrastructure operation, automation and developer experience")
        inceptionYear.set("2019")
        url.set("https://github.com/pellepelster/solidblocks/")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("https://opensource.org/license/mit")
            }
        }

        developers {
            developer {
                id.set("pellepelster")
                name.set("Christian 'Pelle' Pelster")
                url.set("https://github.com/pellepelster/")
            }
        }

        scm {
            url.set("https://github.com/pellepelster/solidblocks/")
            connection.set("scm:git:git://github.com/pellepelster/solidblocks.git")
            developerConnection.set("scm:git:ssh://git@github.com/pellepelster/solidblocks.git")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}


object Versions {
    const val testContainersVersion = "1.17.1"
}

dependencies {
    api("io.kotest:kotest-assertions-core:5.9.1")

    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")

    implementation(project(":solidblocks-ssh"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.slf4j:slf4j-simple:2.0.13")
    implementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    implementation("com.github.docker-java:docker-java:3.4.0")
    implementation("com.github.docker-java:docker-java-transport-zerodep:3.4.0")
}
