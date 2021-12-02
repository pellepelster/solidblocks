package de.solidblocks.cli.commands.environments

import de.solidblocks.cli.cloud.commands.BaseCloudSpringCommand
import de.solidblocks.cloud.config.CloudConfigurationManager
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.Path
import kotlin.io.path.setPosixFilePermissions
import kotlin.system.exitProcess

@Component
class EnvironmentSshConfigCommand :
    BaseCloudSpringCommand(name = "ssh-config", help = "create ssh config") {

    private val logger = KotlinLogging.logger {}

    private fun sshConfig(privateKeyFile: String, knownHostsFile: String): String {
        return """
            Host *
                User root
                IdentitiesOnly yes
                IdentityFile $privateKeyFile
                UserKnownHostsFile $knownHostsFile
        """.trimIndent()
    }

    override fun run() {
        runSpringApplication {
            it.getBean(CloudConfigurationManager::class.java).let {
                val cloud = it.getCloud(cloud) ?: exitProcess(1)

                val basePath = Path(System.getProperty("user.home"), ".solidblocks", this.cloud)
                for (environment in it.listEnvironments(cloud)) {

                    val environmentPath = Path(basePath.toString(), environment.name)

                    if (!environmentPath.toFile().exists() && !environmentPath.toFile().mkdirs()) {
                        logger.error { "creating dir '${environmentPath.toAbsolutePath()}' failed" }
                        exitProcess(1)
                    }

                    val privateKeyFile = File(environmentPath.toFile(), "${this.cloud}_key")
                    privateKeyFile.writeText(environment.sshSecrets.sshPrivateKey)
                    privateKeyFile.toPath().setPosixFilePermissions(PosixFilePermissions.fromString("rw-------"))

                    val knownHostsFile = File(environmentPath.toFile(), "${this.cloud}_known_hosts")
                    knownHostsFile.writeText("vault-1.$environment.${environment.cloud.rootDomain} ${environment.sshSecrets.sshIdentityPublicKey}")

                    knownHostsFile.toPath().setPosixFilePermissions(PosixFilePermissions.fromString("rw-------"))

                    File(
                        environmentPath.toFile(),
                        "${this.cloud}_key.pub"
                    ).writeText(environment.sshSecrets.sshPublicKey)
                    File(environmentPath.toFile(), "ssh_config").writeText(
                        sshConfig(
                            privateKeyFile.absolutePath,
                            knownHostsFile.absolutePath
                        )
                    )
                }
            }
        }
    }
}
