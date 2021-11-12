import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("net.nemerosa.versioning")
    //id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
}

versioning {
}

val versionFile = File("$rootDir/version.txt")
version = versionFile.readText().trim()

val junitJupiterVersion = "5.7.1"

dependencies {
    constraints {
        implementation("org.apache.commons:commons-text:1.9")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.6")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.testcontainers:testcontainers:1.15.3")
    testImplementation("org.testcontainers:junit-jupiter:1.15.3")

}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        suppressWarnings = true
        jvmTarget = "16"
        allWarningsAsErrors = false
        noJdk = false
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Commit"] = versioning.info.commit
        attributes["Solidblocks-Version"] = versioning.info.full
    }
}

/*
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    outputToConsole.set(true)
    outputColorName.set("RED")
    reporters {
        reporter(ReporterType.PLAIN_GROUP_BY_FILE)
    }
    filter {
        //exclude("XX/generated/XX")
        //exclude("XX/target/XX")
    }
}
*/