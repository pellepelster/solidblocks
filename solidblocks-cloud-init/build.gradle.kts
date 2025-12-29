import java.security.MessageDigest

plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
}

version = System.getenv("VERSION") ?: "v0.0.0"

dependencies {
    testImplementation("org.slf4j:slf4j-simple:2.0.13")
    testImplementation("org.testcontainers:testcontainers:1.17.5")
    testImplementation("org.testcontainers:junit-jupiter:1.17.5")
}

val zip = tasks.register<Zip>("zip") {
    archiveBaseName = "blcks-cloud-init"
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

val generate = tasks.register("generate") {
    doLast {
        val shellLibDir = project.rootProject.project(":solidblocks-shell").layout.projectDirectory.dir("lib").asFile

        val includes = listOf(
            shellLibDir.resolve("text-include.sh"),
            shellLibDir.resolve("utils-include.sh"),
            shellLibDir.resolve("log-include.sh"),
            shellLibDir.resolve("curl-include.sh"),
            shellLibDir.resolve("apt.sh"),
            shellLibDir.resolve("package-include.sh")
        )

        val sha256256Sum =
            layout.buildDirectory.file("blcks-cloud-init-${version}.zip").get().asFile.readBytes().hashedWithSha256()

        val bootstrapTemplateContent =
            layout.projectDirectory.file("templates/cloud-init-bootstrap.template").asFile.readText()

        val replacedContent = bootstrapTemplateContent
            .replace("__SOLIDBLOCKS_CLOUD_INIT_CHECKSUM__", sha256256Sum)
            .replace("__SOLIDBLOCKS_VERSION__", version.toString())
            .replace("__CLOUD_INIT_BOOTSTRAP_INCLUDES__", includes.joinToString { it.readText() })

        layout.buildDirectory.file("blcks-cloud-init-bootstrap.sh").get().asFile.also {
            it.writeText(replacedContent)
            it.setExecutable(true)
        }
        val snippetsDir = project.layout.buildDirectory.dir("snippets").get().asFile
        snippetsDir.mkdirs()

        snippetsDir.resolve("blcks-cloud-init-bootstrap-${version}.sh").also {
            it.writeText(replacedContent)
            it.setExecutable(true)
        }
    }
}.get()

tasks.getByName("assemble").dependsOn(generate)
generate.dependsOn(zip)

artifacts {
    archives(zip)
}
