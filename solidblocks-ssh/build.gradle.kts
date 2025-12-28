plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
}

dependencies {
    implementation("org.apache.sshd:sshd-core:2.16.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcprov-ext-jdk18on:1.78.1")

    testImplementation("org.slf4j:slf4j-simple:2.0.13")
    testImplementation("org.testcontainers:testcontainers:1.17.5")
    testImplementation("org.testcontainers:junit-jupiter:1.17.5")
}
