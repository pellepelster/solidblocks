plugins {
    id("org.jetbrains.kotlin.jvm")
}

object Versions {
    const val junitJupiterVersion = "5.11.0"
    const val testContainersVersion = "1.17.1"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.junitJupiterVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junitJupiterVersion}")

}

tasks.test {
    useJUnitPlatform()
    testLogging {
        this.showStandardStreams = true
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
