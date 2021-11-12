plugins {
    id("solidblocks.kotlin-library-conventions")
}

dependencies {
    api(project(":solidblocks-api"))
    implementation("org.freemarker:freemarker:2.3.31")
    implementation("org.jgrapht:jgrapht-core:1.5.1")

    testImplementation("org.springframework.boot:spring-boot-test:2.4.5")

    implementation("org.springframework:spring-context:5.3.6")
    testImplementation("org.springframework:spring-test:5.3.6")
    testImplementation("junit:junit:4.13.2")
}
