plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("com.vanniktech.maven.publish") version "0.29.0"
}

dependencies {
    implementation("org.apache.sshd:sshd-core:2.16.0")
    implementation("org.apache.sshd:sshd-scp:2.16.0")

    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.77")
    implementation("org.bouncycastle:bcprov-ext-jdk18on:1.77")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    testImplementation("org.slf4j:slf4j-simple:2.0.13")
    testImplementation(project(":solidblocks-test"))
}

mavenPublishing {
    coordinates("de.solidblocks", "ssh", "${version}")
}
