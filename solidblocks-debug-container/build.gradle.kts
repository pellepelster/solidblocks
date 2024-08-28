plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.spring") version "2.0.20"
}

group = "de.solidblocks"
version = System.getenv("VERSION") ?: "SNAPSHOT"

repositories {
    mavenCentral()
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val integrationTest = task<Test>("integrationTest") {
    description = "Task to run integration tests"
    group = "verification"

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}


dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.webjars.npm:bootstrap:5.2.2")
    implementation("org.webjars:webjars-locator:0.45")
    implementation("org.webjars.npm:htmx.org:1.8.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    integrationTestImplementation("org.junit.jupiter:junit-jupiter-api")
    integrationTestImplementation("org.junit.jupiter:junit-jupiter-engine")
    integrationTestImplementation("org.testcontainers:testcontainers:1.17.5")
    integrationTestImplementation("org.testcontainers:junit-jupiter:1.17.5")
    integrationTestImplementation("io.rest-assured:kotlin-extensions:5.2.0")
}
