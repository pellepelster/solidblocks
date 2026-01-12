import com.mgd.core.gradle.S3Upload
import java.io.StringWriter
import java.security.MessageDigest

plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("com.vanniktech.maven.publish") version "0.29.0"
    id("com.mgd.core.gradle.s3") version "3.0.2"
}

version = System.getenv("VERSION") ?: "v0.0.0"

dependencies {
    testImplementation("org.slf4j:slf4j-simple:2.0.13")
    testImplementation("org.testcontainers:testcontainers:1.17.5")
    testImplementation("org.testcontainers:junit-jupiter:1.17.5")
    testImplementation(project(":solidblocks-test"))

    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
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
    doLast {
        val shellLibDir = project.rootProject.project(":solidblocks-shell").layout.projectDirectory.dir("lib").asFile

        val includes = listOf(
            shellLibDir.resolve("text.sh"),
            shellLibDir.resolve("utils.sh"),
            shellLibDir.resolve("log.sh"),
            shellLibDir.resolve("curl.sh"),
            shellLibDir.resolve("apt.sh"),
            shellLibDir.resolve("package.sh")
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

        val resourcesDir = project.layout.projectDirectory.dir("src/main/resources").asFile

        resourcesDir.resolve("blcks-cloud-init-bootstrap.sh.template").also {
            it.writeText(bootstrapTemplateSh.toString())
            it.setExecutable(true)
        }
    }
}.get()

tasks.getByName("jar").dependsOn(generate)
tasks.getByName("assemble").dependsOn(generate)
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