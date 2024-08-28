plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.5.2")
    implementation("io.github.oshai:kotlin-logging-jvm:4.0.0-beta-22")
    implementation("ch.qos.logback:logback-classic:1.4.7")
    implementation("aws.sdk.kotlin:s3-jvm:0.26.0-beta")
    implementation("aws.sdk.kotlin:s3:0.26.0-beta")
    implementation("aws.sdk.kotlin:dynamodb:0.26.0-beta")
}

application {
    mainClass.set("de.solidblocks.terraform.CliKt")
}
