plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.5.2")
    implementation("me.tomsdevsn:hetznercloud-api:3.1.0")
    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.7.1")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("ch.qos.logback:logback-classic:1.2.6")
}

application {
    mainClass.set("de.solidblocks.hetzner.nuke.CliKt")
}
