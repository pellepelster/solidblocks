plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("com.vanniktech.maven.publish") version "0.29.0"
}

dependencies {
    testImplementation("org.slf4j:slf4j-simple:2.0.13")
    testImplementation("org.testcontainers:testcontainers:1.17.5")
    testImplementation("org.testcontainers:junit-jupiter:1.17.5")
}

mavenPublishing {
    coordinates("de.solidblocks", "utils", "${version}")
}
