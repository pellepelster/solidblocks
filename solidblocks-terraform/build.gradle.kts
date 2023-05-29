plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
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

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useKotlinTest("1.7.10")

            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-engine:5.9.1")
            }
        }
    }
}

application {
    mainClass.set("de.solidblocks.terraform.CliKt")
}
