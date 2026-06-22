import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("com.vanniktech.maven.publish") version "0.29.0"
}

dependencies {
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    api(project(":solidblocks-api"))
}

mavenPublishing {
    coordinates("de.solidblocks", "api", "${version}")

    pom {
        name.set("Solidblocks API")
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

    if (!project.gradle.startParameter.taskNames.any { it.contains("publishToMavenLocal") }) {
        signAllPublications()
    }
}
