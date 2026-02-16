plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("plugin.spring") version "2.0.20"
}

repositories {
    mavenCentral()
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

    testImplementation("io.rest-assured:kotlin-extensions:5.2.0")
    testImplementation("org.junit.platform:junit-platform-engine:1.12.2")
}
