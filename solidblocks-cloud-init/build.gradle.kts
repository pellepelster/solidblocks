
plugins {
    `maven-publish`
    id("solidblocks.kotlin-library-conventions")
}

sourceSets {
    main {
        resources {
            setSrcDirs(listOf("src"))
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/pellepelster/solidblocks")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}

abstract class GenerateTask @Inject constructor(@get:Input val projectLayout: ProjectLayout) : DefaultTask() {

    @TaskAction
    fun generate() {

        val files = listOf("lib-cloud-init/shell-script-header.sh", "lib-cloud-init/backup-cloud-init-configuration_noescape.sh", "src/lib/configuration.sh", "src/lib/curl.sh", "src/lib/common.sh", "src/lib/network.sh", "src/lib/vault.sh", "lib-cloud-init/backup-cloud-init-body.sh")

        val file_contents = files.map { file ->
            var content = File("${projectLayout.projectDirectory}/${file}").readText(Charsets.UTF_8)

            if (!file.endsWith("_noescape.sh")) {
                content = content.replace("\${", "\$\${")
            }

            content
        }

        File("${projectLayout.projectDirectory}/lib-cloud-init-generated/backup-cloud-init.sh").writeText(file_contents.joinToString("\n"), Charsets.UTF_8)
    }
}
tasks.register<GenerateTask>("generate")

tasks.getByName("build").dependsOn("generate")
