import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("com.vanniktech.maven.publish") version "0.29.0"
}

group = "de.solidblocks"
version = System.getenv("VERSION") ?: "0.0.0"

mavenPublishing {
    coordinates("de.solidblocks", "hetzner-dns", "${version}")

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

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation("org.wiremock:wiremock:3.9.1")
}
