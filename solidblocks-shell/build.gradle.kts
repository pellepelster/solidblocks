import java.security.MessageDigest

plugins {
    id("buildlogic.solidblocks-kotlin-conventions")
    id("buildlogic.solidblocks-releasetest-conventions")
}

version = System.getenv("VERSION") ?: "v0.0.0"

sourceSets {
    main {
        resources {
            setSrcDirs(listOf("src/main/resources", "lib"))
        }
    }
    test {
        resources {
            setSrcDirs(listOf("src/test/resources", "lib"))
        }
    }
}

dependencies {
    testImplementation(project(":solidblocks-test"))
    testImplementation("org.wiremock:wiremock:3.9.1")
    releaseTestImplementation(project(":solidblocks-test"))
}

val zip = tasks.register<Zip>("zip") {
    archiveBaseName = "blcks-shell"
    destinationDirectory = layout.buildDirectory

    from(layout.projectDirectory.dir("lib")) {
        include("*.sh")
        into("blcks-shell")
    }
}.get()

fun ByteArray.hashedWithSha256() =
    MessageDigest.getInstance("SHA-256")
        .digest(this)
        .toHexString()

val generate = tasks.register("generate") {
    doLast {
        val sha256Sum =
            layout.buildDirectory.file("blcks-shell-${version}.zip").get().asFile.readBytes().hashedWithSha256()
        val bootstrapTemplateContent =
            layout.projectDirectory.file("templates/snippets/blcks-shell-bootstrap-solidblocks.sh.template").asFile.readText()

        val replacedContent = bootstrapTemplateContent.replace("__SOLIDBLOCKS_SHELL_CHECKSUM__", sha256Sum)
            .replace("__SOLIDBLOCKS_VERSION__", version.toString())

        val snippetsDir = project.layout.buildDirectory.dir("snippets").get().asFile
        snippetsDir.mkdirs()
        snippetsDir.resolve("blcks-shell-bootstrap-solidblocks-${version}.sh").writeText(replacedContent)

        val kitchenSinkContent =
            layout.projectDirectory.file("templates/snippets/blcks-shell-kitchen-sink.sh.template").asFile.readText()
        snippetsDir.resolve("blcks-shell-kitchen-sink-${version}.sh").also {
            it.writeText(kitchenSinkContent.replace("__BOOTSTRAP_SOLIDBLOCKS_SHELL__", replacedContent))
            it.setExecutable(true)
        }

        val minimalSkeletonContent =
            layout.projectDirectory.file("templates/snippets/blcks-shell-minimal-skeleton-do.template").asFile.readText()
        snippetsDir.resolve("blcks-shell-minimal-skeleton-${version}.sh").also {
            it.writeText(minimalSkeletonContent.replace("__BOOTSTRAP_SOLIDBLOCKS_SHELL__", replacedContent))
            it.setExecutable(true)
        }
    }
}.get()

tasks.getByName("assemble").dependsOn(generate)
generate.dependsOn(zip)

artifacts {
    archives(zip)
}
