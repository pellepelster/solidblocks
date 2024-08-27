plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.0.BETA2")
}
