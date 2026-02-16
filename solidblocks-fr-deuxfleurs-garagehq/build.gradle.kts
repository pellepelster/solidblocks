plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("org.openapi.generator") version "7.18.0"
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "de.solidblocks"
version = System.getenv("VERSION") ?: "0.0.0"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    implementation("com.squareup.moshi:moshi-adapters:1.15.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("${layout.projectDirectory}/garage-admin-v2.json")
    outputDir.set("${project.layout.buildDirectory.get()}/generated")
    apiPackage.set("fr.deuxfleurs.garagehq.api")
    modelPackage.set("fr.deuxfleurs.garagehq.model")
    invokerPackage.set("fr.deuxfleurs.garagehq.client")

    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "library" to "jvm-okhttp4",
            "enumPropertyNaming" to "UPPERCASE",
        )
    )

    additionalProperties.set(
        mapOf(
            "useCoroutines" to "true"
        )
    )
}

val generatedSources: CopySpec = copySpec {
    from("build/generated/src/main/kotlin")
    include("**/*.kt")
}

val copyGenerated = tasks.register<Copy>("copyGenerated") {
    into(layout.projectDirectory.dir("src/main/kotlin"))
    with(generatedSources)
}.get()

copyGenerated.dependsOn("openApiGenerate")
tasks.build.get().dependsOn(copyGenerated)
tasks.getByName("compileKotlin").dependsOn(copyGenerated)

tasks.named("spotlessKotlin") { enabled = false }

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}