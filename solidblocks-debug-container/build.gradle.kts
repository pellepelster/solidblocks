import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.4"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
}

group = "de.solidblocks.debug.container"
version = System.getenv("VERSION") ?: "SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
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
    //integrationTestImplementation("io.rest-assured:xml-path:5.2.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
