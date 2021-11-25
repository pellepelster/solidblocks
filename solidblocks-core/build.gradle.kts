import Constants.SPRING_BOOT_VERSION

plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    api("org.springframework:spring-context:5.3.6")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")

    api("commons-net:commons-net:3.1")
    api("joda-time:joda-time:2.10.10")

    implementation("org.springframework.boot:spring-boot-starter-webflux:${SPRING_BOOT_VERSION}")
    testImplementation("org.hamcrest:hamcrest:2.2")
}
