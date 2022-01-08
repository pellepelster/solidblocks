import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

plugins {
    id("solidblocks.kotlin-library-conventions")
    id("org.openapi.generator") version "5.3.1"
}

val ktorVersion = "1.5.4"

val generatedClient = "client"
val generatedServer = "server"

dependencies {
}

sourceSets.create(generatedClient)
sourceSets.create(generatedServer)

kotlin {
    sourceSets[generatedClient].apply {
        kotlin.srcDir("$buildDir/$generatedClient/src/main/kotlin")

        dependencies {
            implementation("com.squareup.moshi:moshi-kotlin:1.12.0")
            implementation("com.squareup.moshi:moshi-adapters:1.12.0")
            implementation("com.squareup.okhttp3:okhttp:4.9.3")
        }
    }

    sourceSets[generatedServer].apply {
        kotlin.srcDir("$buildDir/$generatedServer/src/main/kotlin")

        dependencies {
            // implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
            implementation("io.ktor:ktor-auth:$ktorVersion")
            implementation("io.ktor:ktor-gson:$ktorVersion")
            implementation("io.ktor:ktor-locations:$ktorVersion")
            implementation("io.dropwizard.metrics:metrics-core:4.1.18")
            implementation("io.ktor:ktor-metrics:$ktorVersion")
            implementation("io.ktor:ktor-server-netty:$ktorVersion")
        }
    }
}

tasks.create<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generate${generatedClient.capitalize()}") {
    group = generatedClient

    inputSpec.set("$projectDir/src/main/resources/solidblocks-ingress-api.yaml")
    generatorName.set("kotlin")
    packageName.set("de.solidblocks.ingress.api.client")
    outputDir.set("$buildDir/$generatedClient")
}

tasks.create<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generate${generatedServer.capitalize()}") {
    group = generatedServer

    inputSpec.set("$projectDir/src/main/resources/solidblocks-ingress-api.yaml")
    generatorName.set("kotlin-server")
    packageName.set("de.solidblocks.ingress.api.server")
    outputDir.set("$buildDir/$generatedServer")
}

for (type in setOf(generatedServer, generatedClient)) {

    configurations.create(type) {
        isCanBeConsumed = true
        isCanBeResolved = false
        // extendsFrom(configurations["implementation"], configurations["runtimeOnly"])
    }

    val jar = tasks.create<Jar>("jar${type.capitalize()}") {
        group = type
        archiveAppendix.set(type.toLowerCaseAsciiOnly())
        from(sourceSets[type].output)
        dependsOn("compile${type.capitalize()}Kotlin")
    }
    tasks.getByPath("jar").dependsOn(jar)

    artifacts.add(type, jar) {
        builtBy(tasks.getByPath("compile${type.capitalize()}Kotlin"))
    }

    tasks.getByPath("compile${type.capitalize()}Kotlin").dependsOn("generate${type.capitalize()}")

    val clean = tasks.create<Delete>("clean${type.capitalize()}") {
        group = type
        delete = setOf("$buildDir/$type")
    }
    tasks.getByPath("clean").dependsOn(clean)
}
