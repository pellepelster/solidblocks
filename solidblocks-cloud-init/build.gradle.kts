import com.mgd.core.gradle.S3Upload
import java.io.StringWriter
import java.security.MessageDigest

plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("com.vanniktech.maven.publish") version "0.29.0"
    id("com.mgd.core.gradle.s3") version "3.0.2"
}

version = "v${System.getenv("VERSION") ?: "0.0.0"}"

dependencies {
    implementation(project(":solidblocks-shell"))
    implementation(project(":solidblocks-ssh"))
    implementation(project(":solidblocks-utils"))
    implementation("com.charleskorn.kaml:kaml:0.83.0")

    testImplementation("org.slf4j:slf4j-simple:2.0.13")
    testImplementation(project(":solidblocks-test"))

    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
    testImplementation("org.postgresql:postgresql:42.7.3")
}

val cloudInitBaseName = "blcks-cloud-init"
val cloudInitZip = layout.buildDirectory.file("${cloudInitBaseName}-${version}.zip")

tasks.jar {
    from(layout.projectDirectory.dir("lib")) {
        include("*.sh")
        into("lib")
    }
}

val zip = tasks.register<Zip>("zip") {
    archiveBaseName = cloudInitBaseName
    destinationDirectory = layout.buildDirectory

    from(layout.projectDirectory.dir("lib")) {
        include("*.sh")
        into("lib")
    }
}.get()

fun ByteArray.hashedWithSha256() =
    MessageDigest.getInstance("SHA-256")
        .digest(this)
        .toHexString()

fun List<File>.processIncludes(sw: StringWriter) {
    this.forEach {
        println("adding include '${it.absolutePath}'")
        sw.appendLine("################################################################")
        sw.appendLine("# ${it.toPath().fileName}")
        sw.appendLine("################################################################")
        sw.appendLine(it.readText().lines().filter { !it.startsWith("source") }.joinToString("\n"))
    }
}

val generate = tasks.register("generate") {
    val shellLibDir = project.rootProject.project(":solidblocks-shell").layout.projectDirectory.dir("lib")

    inputs.files(
        shellLibDir.file("text.sh"),
        shellLibDir.file("utils.sh"),
        shellLibDir.file("log.sh"),
        shellLibDir.file("curl.sh"),
        shellLibDir.file("apt.sh"),
    )
    inputs.file(layout.projectDirectory.file("templates/cloud-init-bootstrap.header.template"))
    inputs.file(layout.projectDirectory.file("templates/cloud-init-bootstrap.body.template"))
    inputs.file(cloudInitZip)

    outputs.file(layout.buildDirectory.file("blcks-cloud-init-bootstrap.sh"))
    outputs.dir(layout.buildDirectory.dir("snippets"))
    outputs.file(layout.buildDirectory.file("generated/resources/main/blcks-cloud-init-bootstrap.sh.template"))

    doLast {
        val includes = listOf(
            shellLibDir.file("text.sh").asFile,
            shellLibDir.file("utils.sh").asFile,
            shellLibDir.file("log.sh").asFile,
            shellLibDir.file("curl.sh").asFile,
            shellLibDir.file("apt.sh").asFile,
        )

        val sha256256Sum = cloudInitZip.get().asFile.readBytes().hashedWithSha256()

        val bootstrapHeaderTemplateContent =
            layout.projectDirectory.file("templates/cloud-init-bootstrap.header.template").asFile.readText()

        val bootstrapBodyTemplateContent =
            layout.projectDirectory.file("templates/cloud-init-bootstrap.body.template").asFile.readText()

        val replacedBootstrapBodyTemplateContent = bootstrapBodyTemplateContent
            .replace("__SOLIDBLOCKS_CLOUD_INIT_CHECKSUM__", sha256256Sum)
            .replace("__SOLIDBLOCKS_VERSION__", version.toString())


        val bootstrapSh = StringWriter()
        bootstrapSh.appendLine(bootstrapHeaderTemplateContent)
        bootstrapSh.appendLine()
        includes.processIncludes(bootstrapSh)
        bootstrapSh.appendLine()
        bootstrapSh.appendLine(replacedBootstrapBodyTemplateContent)

        val bootstrapTemplateSh = StringWriter()
        bootstrapTemplateSh.appendLine(bootstrapHeaderTemplateContent)
        bootstrapTemplateSh.appendLine()
        bootstrapTemplateSh.appendLine("__CLOUD_INIT_VARIABLES__")
        bootstrapTemplateSh.appendLine()
        includes.processIncludes(bootstrapTemplateSh)
        bootstrapTemplateSh.appendLine()
        bootstrapTemplateSh.appendLine(replacedBootstrapBodyTemplateContent)
        bootstrapTemplateSh.appendLine()
        bootstrapTemplateSh.appendLine("__CLOUD_INIT_SCRIPT__")

        layout.buildDirectory.file("blcks-cloud-init-bootstrap.sh").get().asFile.also {
            it.writeText(bootstrapSh.toString())
            it.setExecutable(true)
        }
        val snippetsDir = project.layout.buildDirectory.dir("snippets").get().asFile
        snippetsDir.mkdirs()

        snippetsDir.resolve("blcks-cloud-init-bootstrap-${version}.sh").also {
            it.writeText(bootstrapSh.toString())
            it.setExecutable(true)
        }

        val generatedResourcesDir = layout.buildDirectory.dir("generated/resources/main").get().asFile
        generatedResourcesDir.mkdirs()

        generatedResourcesDir.resolve("blcks-cloud-init-bootstrap.sh.template").also {
            it.writeText(bootstrapTemplateSh.toString())
            it.setExecutable(true)
        }
    }
}.get()

tasks.test {
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
    systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "4")
}

sourceSets.main {
    resources.srcDir(layout.buildDirectory.dir("generated/resources/main"))
}

tasks.getByName("jar").dependsOn(generate)
tasks.getByName("assemble").dependsOn(generate)
tasks.getByName("processResources").dependsOn(generate)
tasks.matching { it.name == "sourcesJar" }.configureEach { dependsOn(generate) }
generate.dependsOn(zip)

artifacts {
    archives(zip)
}

publishing {
    publications {
        register<MavenPublication>("cloudInit") {
            from(components["java"])
        }
    }
}

configurations {
    create("cloud-init")
}

val cloudInitZipArtifact = artifacts.add("cloud-init", cloudInitZip.get().asFile) {
    type = "cloud-init"
    builtBy("zip")
}

mavenPublishing {
    coordinates("de.solidblocks", "cloud-init", "${version}")
}

tasks.register("uploadToTestBucket", S3Upload::class) {
    System.setProperty("aws.accessKeyId", providers.of(PassSecretValueSource::class) {
        this.parameters.path = "solidblocks/aws/test/access_key_id"
    }.get())
    System.setProperty("aws.secretAccessKey", providers.of(PassSecretValueSource::class) {
        this.parameters.path = "solidblocks/aws/test/secret_access_key"
    }.get())

    region = "eu-central-1"
    bucket = "test-blcks-bootstrap"
    key = "blcks-cloud-init-${version}.zip"
    file = cloudInitZip.get().asFile.absolutePath
    overwrite = true
}